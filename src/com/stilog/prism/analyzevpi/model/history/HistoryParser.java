package com.stilog.prism.analyzevpi.model.history;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parseur des fichiers d'historique {@code history.txt} et {@code historytracker.txt}
 * présents dans une archive VPI/VPS.
 *
 * <h2>Format history.txt</h2>
 * <pre>
 *   id ; dateCreation ; "userCreation" ; ; "" ; "type" ; dateModif ; "userModif" ; "guid"
 * </pre>
 *
 * <h2>Format historytracker.txt</h2>
 * <pre>
 *   trackerId ; "guid" ; "entityType" ; date ; "user" ; "base64Xml"
 * </pre>
 * Le XML décodé contient une liste de {@code <PropertyTracker>}.
 *
 * <p>Les deux fichiers sont joints sur le GUID pour enrichir chaque
 * {@link HistoryEntry} avec la liste de ses {@link HistoryTrackerEntry}.
 */
public class HistoryParser {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Parse les deux fichiers et retourne la liste des entrées d'historique
     * enrichies de leurs trackers.
     *
     * @param historyFile        chemin vers {@code history.txt}
     * @param historyTrackerFile chemin vers {@code historytracker.txt}  (peut être null)
     * @return liste triée par dateModification décroissante
     */
    /**
     * @deprecated Utiliser {@link #parseHistory} + {@link HistoryTrackerParser#buildIndex}
     *             + {@link #joinAndSort(List, TrackerIndex)} pour les grands fichiers.
     *             Conservé pour compatibilité.
     */
    @Deprecated
    public static List<HistoryEntry> parse(File historyFile, File historyTrackerFile)
            throws IOException {
        List<HistoryEntry> entries = parseHistory(historyFile);

        if (historyTrackerFile != null && historyTrackerFile.exists()) {
            List<HistoryTrackerEntry> trackers = parseHistoryTracker(historyTrackerFile);
            // Jointure inline (joinTrackers supprimé — remplacé par joinAndSort + TrackerIndex)
            Map<String, HistoryEntry> byGuid = new LinkedHashMap<>();
            for (HistoryEntry e : entries) byGuid.put(e.getGuid(), e);
            for (HistoryTrackerEntry t : trackers) {
                HistoryEntry entry = byGuid.get(t.getGuid());
                if (entry != null) {
                    entry.addTrackerRef(new TrackerRef(
                        t.getTrackerId(), t.getGuid(), t.getEntityType(),
                        t.getDate(), t.getUser(), ""));
                }
            }
        }

        entries.sort(Comparator.comparing(HistoryEntry::getDateModification,
                                          Comparator.nullsLast(Comparator.reverseOrder())));
        return entries;
    }

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

    // ── Parsing historytracker.txt ────────────────────────────────────────────

    /**
     * Parse historytracker.txt.
     * Colonnes (index 0-based, séparateur ";") :
     *   0=trackerId  1=guid  2=entityType  3=date  4=user  5=base64Xml
     */
    public static List<HistoryTrackerEntry> parseHistoryTracker(File file) throws IOException {
        List<HistoryTrackerEntry> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = splitSemicolon(line);
                if (cols.length < 6) continue;

                try {
                    long   trackerId  = Long.parseLong(cols[0].trim());
                    String guid       = clean(cols[1]);
                    String entityType = clean(cols[2]);
                    String dateStr    = clean(cols[3]);
                    String user       = clean(cols[4]);
                    String b64        = clean(cols[5]);

                    LocalDateTime date    = parseDate(dateStr);
                    List<PropertyChange> changes = decodeChanges(b64);

                    list.add(new HistoryTrackerEntry(trackerId, guid, entityType, date, user, changes));
                } catch (NumberFormatException ignored) {
                    // ligne mal formée
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

    /**
     * @deprecated Utiliser {@link #joinAndSort(List, TrackerIndex)} avec le
     * nouveau {@link HistoryTrackerParser}.
     */
    @Deprecated
    public static void joinAndSort(List<HistoryEntry> entries,
                                   List<HistoryTrackerEntry> trackers) {
        // Compat legacy : convertir en TrackerIndex minimal (sans offsets réels)
        TrackerIndex idx = new TrackerIndex();
        for (HistoryTrackerEntry t : trackers) {
            idx.add(t.getGuid().toUpperCase(java.util.Locale.ROOT),
                new TrackerRef(t.getTrackerId(), t.getGuid(), t.getEntityType(),
                               t.getDate(), t.getUser(), ""));
        }
        joinAndSort(entries, idx);
    }

    // ── Décodage XML base64 ───────────────────────────────────────────────────

    /**
     * Décode le payload base64, parse le XML et retourne la liste des
     * {@link PropertyChange}.
     */
    static List<PropertyChange> decodeChanges(String base64) {
        List<PropertyChange> result = new ArrayList<>();
        if (base64 == null || base64.isBlank()) return result;
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String xml     = new String(decoded, StandardCharsets.UTF_8);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db  = dbf.newDocumentBuilder();
            Document        doc = db.parse(new InputSource(new StringReader(xml)));

            NodeList trackers = doc.getElementsByTagName("PropertyTracker");
            for (int i = 0; i < trackers.getLength(); i++) {
                Element el   = (Element) trackers.item(i);
                String prop  = textContent(el, "propertyName");
                String old_  = textContent(el, "oldValue");
                String new_  = textContent(el, "newValue");
                if (prop != null) result.add(new PropertyChange(prop, old_, new_));
            }
        } catch (Exception ignored) {
            // XML malformé ou base64 invalide
        }
        return result;
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

    private static String textContent(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent();
    }
}
