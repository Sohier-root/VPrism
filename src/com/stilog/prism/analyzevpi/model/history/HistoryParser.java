package com.stilog.prism.analyzevpi.model.history;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Parseur du fichier d'historique {@code history.txt} présent dans une
 * archive VPI/VPS.
 *
 * <h2>Format history.txt</h2>
 * <pre>
 *   id ; dateCreation ; "userCreation" ; ; "" ; "type" ; dateModif ; "userModif" ; "guid"
 * </pre>
 *
 * <p>Les entrées sont enrichies avec leurs {@link TrackerRef} (métadonnées de
 * {@code historytracker.txt}, indexées par {@link HistoryTrackerParser}) via
 * {@link #joinAndSort(List, TrackerIndex)}.
 */
public class HistoryParser {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Parsing history.txt ───────────────────────────────────────────────────

    /**
     * Parse history.txt.
     * Colonnes (index 0-based, séparateur ";") :
     *   0=id  1=dateCreation  2=userCreation  3=(vide)  4=(vide/\"\")
     *   5=type  6=dateModif  7=userModif  8=guid
     */
    public static List<HistoryEntry> parseHistory(File file) throws IOException {
        List<HistoryEntry> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = splitSemicolon(line);
                if (cols.length < 9) continue;

                try {
                    int    id       = Integer.parseInt(cols[0].trim());
                    String dateCre  = clean(cols[1]);
                    String userCre  = clean(cols[2]);
                    String type     = clean(cols[5]);
                    String dateMod  = clean(cols[6]);
                    String userMod  = clean(cols[7]);
                    String guid     = clean(cols[8]);

                    // Dédoublonner sur (id + type + guid) — quelques lignes sont dupliquées
                    String key = id + "|" + type + "|" + guid;
                    if (seen.contains(key)) continue;
                    seen.add(key);

                    LocalDateTime dCreation     = parseDate(dateCre);
                    LocalDateTime dModification = parseDate(dateMod);

                    list.add(new HistoryEntry(id, dCreation, userCre,
                                              type, dModification, userMod, guid));
                } catch (NumberFormatException ignored) {
                    // ligne mal formée, on l'ignore
                }
            }
        }
        return list;
    }

    // ── Jointure GUID ─────────────────────────────────────────────────────────

    /**
     * Jointure GUID + tri par date de modification décroissante.
     * Utilise le {@link TrackerIndex} léger : attache des {@link TrackerRef}
     * aux entrées, sans charger les {@link PropertyChange} en mémoire.
     *
     * @param entries  liste d'entrées history (modifiée en place)
     * @param index    index buildé par {@link HistoryTrackerParser#buildIndex}
     */
    public static void joinAndSort(List<HistoryEntry> entries, TrackerIndex index) {
        for (HistoryEntry e : entries) {
            for (TrackerRef ref : index.get(e.getGuid())) {
                e.addTrackerRef(ref);
            }
        }
        entries.sort(Comparator.comparing(
            HistoryEntry::getDateModification,
            Comparator.nullsLast(Comparator.reverseOrder())));
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /** Découpe sur ';' en respectant les guillemets. */
    private static String[] splitSemicolon(String line) {
        // Les valeurs peuvent contenir des guillemets mais pas de ; imbriqué
        // → split simple suffisant ici
        return line.split(";", -1);
    }

    /** Supprime les guillemets doubles encadrants et les espaces. */
    static String clean(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
            s = s.substring(1, s.length() - 1);
        return s.trim();
    }

    private static LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s, DT_FMT); }
        catch (DateTimeParseException e) { return null; }
    }
}
