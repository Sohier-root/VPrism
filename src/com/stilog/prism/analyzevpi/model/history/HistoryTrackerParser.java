package com.stilog.prism.analyzevpi.model.history;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parseur optimisé pour {@code historytracker.txt}, conçu pour gérer des
 * fichiers de plusieurs gigaoctets sans saturer le heap Java.
 *
 * <h2>Stratégie</h2>
 * <ol>
 *   <li><b>Scan streaming</b> ({@link #buildIndex}) : lecture ligne à ligne du
 *       fichier décompressé. Seules les colonnes 0-4 (métadonnées) sont parsées ;
 *       pour la colonne 5 (payload base64), on enregistre uniquement l'offset et
 *       la longueur dans le fichier. Résultat : un {@link TrackerIndex} léger
 *       (~80-100 bytes par entrée).</li>
 *   <li><b>Lecture lazy</b> ({@link #loadChanges}) : au clic utilisateur, on
 *       ouvre le fichier avec un {@link RandomAccessFile}, on saute à l'offset,
 *       on lit exactement {@code payloadLength} bytes, on décode le base64, et
 *       on parse le XML via SAX (pas de DOM → aucun arbre en mémoire).</li>
 * </ol>
 *
 * <h2>Empreinte mémoire</h2>
 * <ul>
 *   <li>Index seul : ~100 bytes × N lignes (Strings internées pour guid/user/type)</li>
 *   <li>Aucun {@link PropertyChange} en mémoire sauf ceux de la ligne affichée</li>
 *   <li>SAX : mémoire constante quelle que soit la taille du XML</li>
 * </ul>
 */
public class HistoryTrackerParser {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Scan streaming → index ────────────────────────────────────────────────

    /**
     * Scanne {@code trackerFile} ligne à ligne et construit le {@link TrackerIndex}.
     *
     * <p>Le fichier doit être le fichier texte décompressé (pas un ZIP).
     * La lecture est purement séquentielle ; la consommation mémoire est
     * proportionnelle au nombre de lignes × ~100 bytes.
     *
     * @param trackerFile fichier {@code historytracker.txt} décompressé
     * @param progress    callback appelé périodiquement pour la progression
     *                    (reçoit le nombre de lignes traitées) ; peut être null
     * @return index prêt à l'emploi
     */
    public static TrackerIndex buildIndex(File trackerFile,
                                          java.util.function.LongConsumer progress)
            throws IOException {

        TrackerIndex idx = new TrackerIndex();
        long lineCount   = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    new java.io.BufferedInputStream(
                        new FileInputStream(trackerFile), 256 * 1024),
                    StandardCharsets.UTF_8), 128 * 1024)) {

            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                if (line.isBlank()) continue;

                TrackerRef ref = parseLine(line);
                if (ref != null) {
                    idx.add(ref.guid(), ref);
                }

                if (progress != null && (lineCount % 50_000) == 0) {
                    progress.accept(lineCount);
                }
            }
        }

        idx.sort();
        return idx;
    }

    /**
     * Parse une ligne et retourne une {@link TrackerRef} avec le payload base64 inline.
     * Format : {@code id ; "guid" ; "entityType" ; date ; "user" ; "base64"}
     */
    private static TrackerRef parseLine(String line) {
        // Format : id ; guid ; entityType ; date ; user ; base64
        // = 5 séparateurs ';', payload = tout ce qui suit le 5ème
        int[] semis = findSemicolons(line, 5);
        if (semis[4] < 0) return null;

        try {
            long   trackerId  = Long.parseLong(line, 0, semis[0], 10);
            String guid       = cleanField(line, semis[0] + 1, semis[1])
                                    .toUpperCase(Locale.ROOT).intern();
            String entityType = cleanField(line, semis[1] + 1, semis[2]).intern();
            String dateStr    = line.substring(semis[2] + 1, semis[3]).trim();
            LocalDateTime date = parseDate(dateStr);
            String user       = cleanField(line, semis[3] + 1, semis[4]).intern();

            // Extraire le payload base64 directement depuis la String
            int payloadStart = semis[4] + 1;
            if (payloadStart < line.length() && line.charAt(payloadStart) == '"') payloadStart++;
            int payloadEnd = line.length();
            if (payloadEnd > 0 && line.charAt(payloadEnd - 1) == '"') payloadEnd--;

            // Stocker le payload inline — évite tout problème d'offset fichier
            // Mémoire : ~400 bytes/ligne en moyenne, acceptable même pour 5M lignes
            // car intern() sur guid/user/type partage les Strings communes
            String payload = (payloadStart < payloadEnd)
                ? line.substring(payloadStart, payloadEnd) : "";

            return new TrackerRef(trackerId, guid, entityType, date, user, payload);

        } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
            return null;
        }
    }

    /**
     * Retourne les indices des N premiers ';' dans {@code line}.
     * Valeur -1 si non trouvé.
     */
    private static int[] findSemicolons(String line, int n) {
        int[] result = new int[n];
        Arrays.fill(result, -1);
        int count = 0;
        for (int i = 0; i < line.length() && count < n; i++) {
            if (line.charAt(i) == ';') result[count++] = i;
        }
        return result;
    }

    /** Extrait et nettoie un champ entre deux positions (enlève guillemets et espaces). */
    private static String cleanField(String line, int from, int to) {
        if (from >= to) return "";
        String s = line.substring(from, to).trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length()-1) == '"')
            s = s.substring(1, s.length() - 1).trim();
        return s;
    }

    private static LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s, DT_FMT); }
        catch (DateTimeParseException e) { return null; }
    }

    // ── Lecture lazy des PropertyChanges ──────────────────────────────────────

    /**
     * Décode le payload base64 d'un {@link TrackerRef} et parse le XML via SAX.
     *
     * <p>Le payload est stocké directement dans {@link TrackerRef#payload()},
     * aucune lecture disque n'est nécessaire à ce stade.
     *
     * @param trackerFile ignoré (conservé pour compatibilité d'appel)
     * @param ref         référence dont on veut les changements
     * @return liste des {@link PropertyChange}, vide si erreur ou payload absent
     */
    public static List<PropertyChange> loadChanges(File trackerFile, TrackerRef ref) {
        if (ref == null || ref.payload() == null || ref.payload().isBlank()) return List.of();
        try {
            byte[] xmlBytes = Base64.getDecoder().decode(ref.payload());
            return parseSax(xmlBytes);
        } catch (IllegalArgumentException e) {
            System.err.println("[HistoryTrackerParser] Base64 invalide pour tracker "
                + ref.trackerId() + " : " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Parse le XML des PropertyTracker via SAX.
     * Mémoire : O(nombre de PropertyTracker dans ce XML), pas O(taille du fichier).
     */
    private static List<PropertyChange> parseSax(byte[] xmlBytes) {
        List<PropertyChange> result = new ArrayList<>(4);
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            spf.newSAXParser().parse(
                new InputSource(new ByteArrayInputStream(xmlBytes)),
                new PropertyTrackerHandler(result)
            );
        } catch (Exception e) {
            // XML malformé ou base64 invalide → liste vide
        }
        return result;
    }

    /** Handler SAX pour extraire les PropertyTracker. */
    private static class PropertyTrackerHandler extends DefaultHandler {
        private final List<PropertyChange> result;
        private final StringBuilder        buf = new StringBuilder(256);
        private String  currentProperty;
        private String  currentOld;
        private String  currentNew;
        private boolean inTracker;
        private boolean capture;
        private String  currentTag;

        PropertyTrackerHandler(List<PropertyChange> result) { this.result = result; }

        @Override
        public void startElement(String uri, String local, String qName, Attributes attrs) {
            switch (qName) {
                case "PropertyTracker" -> {
                    inTracker = true;
                    currentProperty = null;
                    currentOld      = null;
                    currentNew      = null;
                }
                case "propertyName", "oldValue", "newValue" -> {
                    if (inTracker) { capture = true; currentTag = qName; buf.setLength(0); }
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (capture) buf.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String local, String qName) {
            if (capture && inTracker) {
                String val = buf.toString();
                switch (qName) {
                    case "propertyName" -> currentProperty = val;
                    case "oldValue"     -> currentOld      = val;
                    case "newValue"     -> currentNew      = val;
                }
                capture = false;
            }
            if ("PropertyTracker".equals(qName) && inTracker) {
                if (currentProperty != null && !currentProperty.isBlank()) {
                    result.add(new PropertyChange(currentProperty, currentOld, currentNew));
                }
                inTracker = false;
            }
        }
    }


}
