package com.stilog.prism.analyzevpi.model.history;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import java.time.LocalDateTime;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * Résout les identifiants techniques présents dans l'historique VPS en noms
 * lisibles par un humain.
 *
 * <h2>Stratégie de résolution pour les EventResourceX</h2>
 * Le label d'une ressource est construit à partir des {@code keyHeadings}
 * définis dans le XML du {@code ResourceModel} correspondant.
 *
 * <p>Chaque keyHeading peut être de type différent :
 * <ul>
 *   <li><b>StringType</b> → valeur textuelle directe (ex. "PARIS")</li>
 *   <li><b>ResourceReference</b> → ID d'une ressource d'une autre dimension :
 *       résolu récursivement via {@link #guidToLabel} / {@link #typeIdToLabel}
 *       (ex. rub20=4 → label de eventresource4 id=4 = "NETWORK-EST")</li>
 *   <li><b>DoubleType / IntegerType / DateTimeType / UniqueChoiceType</b> →
 *       valeur brute affichée telle quelle</li>
 * </ul>
 *
 * <p>Le label final = toutes les KEYs concaténées avec {@code ", "}, suivi
 * optionnellement du premier descriptif STRING non-KEY non vide.
 */
public class VpsLabelResolver {

    // ── Logger fichier ────────────────────────────────────────────────────────
    private static final java.io.File LOG_FILE =
        new java.io.File(System.getProperty("java.io.tmpdir"), "vpslabelresolver_debug.log");

    private static void log(String msg) {
        System.err.println(msg);
        try (java.io.FileWriter fw = new java.io.FileWriter(LOG_FILE, true)) {
            fw.write(LocalDateTime.now() + " " + msg + "\n");
        } catch (Exception ignored) {}
    }

    // ── Colonnes techniques fixes ──────────────────────────────────────────────
    private static final Set<String> FIXED_COLS = Set.of(
        "ID", "UID", "argb", "pattern",
        "dailyCalendar_ID", "eventCreationRule_ID", "hourlyCalendar_ID",
        "externalData", "openForum", "lastPostDate", "lastPostUserName"
    );

    // ── Tables "simples" : col[0]=id, col[1]=nom ──────────────────────────────
    private static final Set<String> SIMPLE_TABLES = Set.of(
        "resourcemodel", "formmodel", "displayperspective", "viewperspective",
        "eventresourceeditormodel", "eventresourcefilter", "planningeventfilter",
        "planningeventeditormodel", "exporteventresourcecontext",
        "importeventresourcecontext", "exporteventcontext", "importeventcontext",
        "workloaddefinition", "dailycalendar", "eventtreestruct",
        "eventcreationrule", "hourlycalendar", "periodstate",
        "planningeventlink", "workflowdefinition", "eventvalue", "eventvaluemodel"
    );

    // ── Index ─────────────────────────────────────────────────────────────────
    /** GUID (UID dans le VPS, uppercase) → label lisible. */
    private final Map<String, String>  guidToLabel    = new HashMap<>();
    /** "tableName|numericId" → label lisible. */
    private final Map<String, String>  typeIdToLabel  = new HashMap<>();
    /** ResourceModel id → nom de la dimension. */
    private final Map<Integer, String> dimensionNames = new HashMap<>();

    // ── Schémas calculés depuis tables.xml + resourcemodel.txt ───────────────

    /**
     * Pour chaque eventresourceX : liste ordonnée des infos de colonnes KEY.
     * Inclut tous les types (String, ResourceReference, Double…).
     */
    private final Map<String, List<KeyColInfo>> keyColsByTable  = new HashMap<>();

    /**
     * Pour chaque eventresourceX : liste ordonnée des indices de colonnes STRING
     * candidates comme descriptif (non-KEY, non-fixe).
     */
    private final Map<String, List<Integer>> descColsByTable = new HashMap<>();

    /** Schémas colName→index par table, partagé entre les deux passes. */
    private final Map<String, Map<String, Integer>> tableColIndexes = new HashMap<>();

    // ── EventTreeStruct et PlanningEventInfo ──────────────────────────────────

    /** Arbres de création d'événements, indexés par id (String). */
    private final List<EventTreeStruct> eventTrees = new ArrayList<>();

    /**
     * PlanningEvent enrichis : UID (uppercase) → PlanningEventInfo.
     * Construit en passe 3 après que les labels de ressources sont disponibles.
     */
    private final Map<String, PlanningEventInfo> eventInfoByUid = new HashMap<>();

    /**
     * Mapping rmId → colonne Resource dans planningevent.txt.
     * Ex : "4" → 24  (Resource4 = col index 24)
     */
    private final Map<String, Integer> rmIdToPeCol = new HashMap<>();

    // ── Records internes ──────────────────────────────────────────────────────

    /**
     * Métadonnées d'un heading keyHeading.
     *
     * @param colIndex     index de la colonne dans le fichier eventresourceX.txt
     * @param typeClass    classe VP du type (StringType, ResourceReference, …)
     * @param refTableName pour ResourceReference : table cible (ex. "eventresource4"), sinon null
     */
    private record KeyColInfo(int colIndex, String typeClass, String refTableName) {}

    /**
     * Métadonnées complètes d'un heading (pour la construction interne).
     *
     * @param columnName   nom de colonne SQL (ex. "rub20")
     * @param isString     vrai si StringType
     * @param typeClass    classe VP du type
     * @param refTableName table cible pour ResourceReference, sinon null
     */
    private record HeadingInfo(String columnName, boolean isString,
                               String typeClass, String refTableName) {}

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Charge tous les index depuis le fichier VPS (ZIP).
     * Peut être appelé depuis n'importe quel thread.
     */
    public void load(File vpsFile) throws IOException {
        if (vpsFile == null || !vpsFile.exists()) return;
        log("[VpsLabelResolver] load() démarré pour: " + vpsFile.getAbsolutePath());
        log("[VpsLabelResolver] Log écrit dans: " + LOG_FILE.getAbsolutePath());

        // Passe 1 : tables.xml + resourcemodel.txt + eventtreestruct.txt → schémas
        Map<String, List<Integer>> allStringCols = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(vpsFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = baseName(entry);
                if ("tables".equals(name)) {
                    byte[] data = readEntry(zis);
                    allStringCols.putAll(parseTableSchema(data));
                    buildRmIdToPeColMap(); // construit après parseTableSchema
                    log("[VpsLabelResolver] Après buildRmIdToPeColMap: rmIdToPeCol=" + rmIdToPeCol.size()
                        + " tableColIndexes contient planningevent=" + tableColIndexes.containsKey("planningevent"));
                } else if ("resourcemodel".equals(name)) {
                    parseResourceModels(readEntry(zis));
                } else if ("eventtreestruct".equals(name)) {
                    parseEventTreeStructs(readEntry(zis));
                    log("[VpsLabelResolver] Après parseEventTreeStructs: " + eventTrees.size() + " arbres");
                }
            }
        }

        // Identifier les tables avec ResourceReference dans leurs keyHeadings
        // (elles doivent être chargées APRÈS leurs tables cibles)
        Set<String> tablesWithRef = new HashSet<>();
        for (Map.Entry<String, List<KeyColInfo>> e : keyColsByTable.entrySet()) {
            for (KeyColInfo kci : e.getValue()) {
                if ("ResourceReference".equals(kci.typeClass())) {
                    tablesWithRef.add(e.getKey());
                    break;
                }
            }
        }

        // Passe 2 : simples + eventresourceX SANS ResourceReference
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(vpsFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = baseName(entry);
                if (SIMPLE_TABLES.contains(name)) {
                    loadSimpleTable(name, readEntry(zis));
                } else if (name.matches("eventresource\\d+") && !tablesWithRef.contains(name)) {
                    List<KeyColInfo> keyCols  = keyColsByTable.getOrDefault(name, List.of());
                    List<Integer>    descCols = descColsByTable.getOrDefault(name,
                        allStringCols.getOrDefault(name, List.of(10)));
                    loadEventResourceTable(name, readEntry(zis), keyCols, descCols);
                } else if ("planningevent".equals(name)) {
                    loadPlanningEventTable(readEntry(zis));
                }
            }
        }

        // Passe 3 : tables avec ResourceReference
        log("[VpsLabelResolver] Passe 3: tablesWithRef=" + tablesWithRef
            + ", typeIdToLabel.size=" + typeIdToLabel.size());
        if (!tablesWithRef.isEmpty()) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(vpsFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = baseName(entry);
                    if (tablesWithRef.contains(name)) {
                        List<KeyColInfo> keyCols  = keyColsByTable.get(name);
                        List<Integer>    descCols = descColsByTable.getOrDefault(name,
                            allStringCols.getOrDefault(name, List.of(10)));
                        loadEventResourceTable(name, readEntry(zis), keyCols, descCols);
                    }
                }
            }
        }

        // Passe 4 : planningevent.txt avec enrichissement complet
        // (après que tous les labels de ressources soient disponibles)
        log("[VpsLabelResolver] Avant passe 4: eventTrees=" + eventTrees.size()
            + " rmIdToPeCol=" + rmIdToPeCol.size()
            + " eventInfoByUid=" + eventInfoByUid.size());
        if (!eventTrees.isEmpty() && !rmIdToPeCol.isEmpty()) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(vpsFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if ("planningevent".equals(baseName(entry))) {
                        loadPlanningEventTableEnriched(readEntry(zis));
                        // Log détaillé par arbre
                        Map<String, Long> byTree = new java.util.HashMap<>();
                        for (PlanningEventInfo pi : eventInfoByUid.values()) {
                            String tName = pi.treeLabel() != null ? pi.treeLabel() : "(no tree)";
                            byTree.merge(tName, 1L, Long::sum);
                        }
                        log("[VpsLabelResolver] Après passe 4: eventInfoByUid=" + eventInfoByUid.size()
                            + " répartition=" + byTree);
                        break;
                    }
                }
            }
        } else {
            // Fallback : rmIdToPeCol vide → charger planningevent en mode basique
            log("[VpsLabelResolver] FALLBACK passe 4 basique (rmIdToPeCol vide — vérifier tables.xml)");
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(vpsFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if ("planningevent".equals(baseName(entry))) {
                        loadPlanningEventTable(readEntry(zis));
                        break;
                    }
                }
            }
        }
    }

    public String resolveGuid(String guid) {
        if (guid == null || guid.isBlank()) return null;
        return guidToLabel.get(guid.toUpperCase(Locale.ROOT));
    }

    public String resolveTypeAndId(String historyType, int numericId) {
        if (historyType == null) return null;
        return typeIdToLabel.get(historyType.toLowerCase(Locale.ROOT) + "|" + numericId);
    }

    public String resolveDimension(int id) {
        return dimensionNames.get(id);
    }

    public boolean isLoaded() {
        return !guidToLabel.isEmpty() || !typeIdToLabel.isEmpty();
    }

    /** Retourne les informations enrichies d'un PlanningEvent par son UID, ou null. */
    public PlanningEventInfo getEventInfo(String uid) {
        if (uid == null || uid.isBlank()) return null;
        return eventInfoByUid.get(uid.toUpperCase(Locale.ROOT));
    }

    /** Nombre de PlanningEventInfo construits (pour diagnostic). */
    public int getEventInfoCount() { return eventInfoByUid.size(); }

    /** Retourne tous les arbres de création d'événements. */
    public List<EventTreeStruct> getEventTrees() {
        return Collections.unmodifiableList(eventTrees);
    }

    // ── Parsing tables.xml ────────────────────────────────────────────────────

    private Map<String, List<Integer>> parseTableSchema(byte[] data) {
        Map<String, List<Integer>> result = new HashMap<>();
        try {
            Document doc = newDocumentBuilder().parse(
                new InputSource(new ByteArrayInputStream(data)));

            NodeList tables = doc.getElementsByTagName("SQLT");
            for (int t = 0; t < tables.getLength(); t++) {
                Element tableEl = (Element) tables.item(t);
                // IMPORTANT : utiliser directChildText et non firstText (récursif)
                // car les <SQLC> enfants ont aussi des <name> qui seraient trouvés en premier
                String tname = directChildText(tableEl, "name").toLowerCase(Locale.ROOT);

                Map<String, Integer> colIndex = new LinkedHashMap<>();
                List<Integer> stringCols = new ArrayList<>();
                NodeList cols = tableEl.getElementsByTagName("SQLC");
                for (int c = 0; c < cols.getLength(); c++) {
                    Element colEl = (Element) cols.item(c);
                    String cname = directChildText(colEl, "name");
                    String ctype = directChildText(colEl, "sqlType");
                    colIndex.put(cname, c);
                    if (!FIXED_COLS.contains(cname) && "1".equals(ctype)) {
                        stringCols.add(c);
                    }
                }
                if (tname.matches("eventresource\\d+")) {
                    if (!stringCols.isEmpty()) result.put(tname, stringCols);
                    tableColIndexes.put(tname, colIndex);
                } else if ("planningevent".equals(tname)) {
                    // Stocker aussi planningevent pour buildRmIdToPeColMap()
                    tableColIndexes.put(tname, colIndex);
                }
            }
        } catch (Exception e) {
            System.err.println("[VpsLabelResolver] Erreur parsing tables.xml : " + e.getMessage());
        }
        return result;
    }

    // ── Parsing resourcemodel.txt ─────────────────────────────────────────────

    private void parseResourceModels(byte[] data) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 4) continue;
                try {
                    int    rmId   = Integer.parseInt(cols[0].trim());
                    String rmName = clean(cols[1]);
                    String b64    = clean(cols[3]);
                    if (rmName.isBlank() || b64.isBlank()) continue;
                    dimensionNames.put(rmId, rmName);
                    parseOneResourceModel(rmId, b64);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[VpsLabelResolver] Erreur lecture resourcemodel : " + e.getMessage());
        }
    }

    private void parseOneResourceModel(int rmId, String b64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(b64);
            Document doc   = newDocumentBuilder().parse(
                new InputSource(new StringReader(
                    new String(decoded, StandardCharsets.UTF_8).replace("\r\n", "\n"))));

            String tname    = "eventresource" + rmId;
            Map<String, Integer> colIndex = tableColIndexes.get(tname);
            if (colIndex == null) return;

            // Construire la map heading_id → HeadingInfo
            Map<String, HeadingInfo> headings = new LinkedHashMap<>();
            NodeList headingNodes = doc.getElementsByTagName("com.visualplanning.data.Heading");
            for (int i = 0; i < headingNodes.getLength(); i++) {
                Element h   = (Element) headingNodes.item(i);
                // <ID> doit être l'enfant direct du heading, pas celui de <type>
                String hid  = directChildText(h, "ID");
                // <columnName> est dans <type><dbType><columnName> → firstText OK
                String col  = firstText(h, "columnName");
                if (hid.isBlank() || col.isBlank()) continue;

                // Détecter le type via l'attribut class du tag <type>
                String typeClass    = "";
                String refTableName = null;
                boolean isString    = false;
                Element typeEl = firstElement(h, "type");
                if (typeEl != null) {
                    typeClass = typeEl.getAttribute("class");
                    isString  = "StringType".equals(typeClass);
                    // ResourceReference : récupérer la table cible
                    if ("ResourceReference".equals(typeClass)) {
                        String refRmId = firstText(typeEl, "entityID");
                        if (!refRmId.isBlank() && !"-1".equals(refRmId)) {
                            refTableName = "eventresource" + refRmId;
                        }
                    }
                }
                // Fallback sqlType=1 si class absent
                if (!isString && typeClass.isBlank()) {
                    isString = "1".equals(firstText(h, "sqlType"));
                    if (isString) typeClass = "StringType";
                }
                headings.put(hid, new HeadingInfo(col, isString, typeClass, refTableName));
            }

            // keyHeadings → liste ordonnée de KeyColInfo (TOUS les types inclus)
            List<KeyColInfo> keyCols       = new ArrayList<>();
            Set<String>      keyColNames   = new LinkedHashSet<>();
            Element keyHeadingsEl = firstElement(doc.getDocumentElement(), "keyHeadings");
            log("[DEBUG RM" + rmId + "] headings parsés: " + headings.size()
                + ", keyHeadingsEl=" + (keyHeadingsEl != null ? "présent" : "ABSENT"));
            if (keyHeadingsEl != null) {
                NodeList items = keyHeadingsEl.getChildNodes();
                for (int i = 0; i < items.getLength(); i++) {
                    if (!(items.item(i) instanceof Element el)) continue;
                    String eid = firstText(el, "entityID");
                    if (eid.isBlank() || "-1".equals(eid)) continue;
                    HeadingInfo hi = headings.get(eid);
                    if (hi == null) continue;
                    Integer idx = colIndex.get(hi.columnName());
                    if (idx != null) {
                        keyCols.add(new KeyColInfo(idx, hi.typeClass(), hi.refTableName()));
                        keyColNames.add(hi.columnName());
                    }
                }
            }

            // Colonnes STRING non-KEY → descriptif
            List<Integer> descCols = new ArrayList<>();
            for (HeadingInfo hi : headings.values()) {
                if (!hi.isString()) continue;
                if (FIXED_COLS.contains(hi.columnName())) continue;
                if (keyColNames.contains(hi.columnName())) continue;
                Integer idx = colIndex.get(hi.columnName());
                if (idx != null) descCols.add(idx);
            }

            if (!keyCols.isEmpty()) {
                keyColsByTable.put(tname, keyCols);
                log("[VpsLabelResolver] RM" + rmId + " → " + tname
                    + " keyCols=" + keyCols.size() + " descCols=" + descCols.size());
            }
            if (!descCols.isEmpty()) descColsByTable.put(tname, descCols);

        } catch (Exception e) {
            System.err.println("[VpsLabelResolver] Erreur ResourceModel id=" + rmId
                + " : " + e.getClass().getSimpleName() + " – " + e.getMessage());
        }
    }

    // ── Chargement des tables de données ─────────────────────────────────────

    /** Table simple : col[0]=id, col[1]=nom. */
    private void loadSimpleTable(String tableName, byte[] data) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 2) continue;
                try {
                    int    id    = Integer.parseInt(cols[0].trim());
                    String label = clean(cols[1]);
                    if (label.isBlank()) continue;
                    typeIdToLabel.put(tableName + "|" + id, label);
                    if ("resourcemodel".equals(tableName)) dimensionNames.put(id, label);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[VpsLabelResolver] Erreur lecture " + tableName + " : " + e.getMessage());
        }
    }

    /**
     * Table eventresourceX.
     *
     * <p>Pour chaque keyColInfo :
     * <ul>
     *   <li><b>StringType</b> → valeur textuelle directe</li>
     *   <li><b>ResourceReference</b> → résolution via {@link #typeIdToLabel}
     *       sur la table cible (ex. "eventresource4|24")</li>
     *   <li><b>Autres (Double, Integer, UniqueChoice…)</b> → valeur brute</li>
     * </ul>
     * Toutes les KEYs sont concaténées avec {@code ", "}.
     * Le premier descriptif STRING non-KEY est ajouté après une {@code ", "}.
     */
    private void loadEventResourceTable(String tableName, byte[] data,
                                        List<KeyColInfo> keyCols, List<Integer> descCols) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 2) continue;

                String uid = clean(cols[1]).toUpperCase(Locale.ROOT);
                if (uid.isBlank()) continue;

                // Construire le label KEY
                StringBuilder keyParts = new StringBuilder();
                for (KeyColInfo kci : keyCols) {
                    if (kci.colIndex() >= cols.length) continue;
                    String raw = clean(cols[kci.colIndex()]);
                    if (raw.isBlank() || raw.startsWith("#ZIP#")) continue;

                    String keyVal;
                    if ("ResourceReference".equals(kci.typeClass()) && kci.refTableName() != null) {
                        // Résoudre la référence vers une autre table
                        String lookupKey = kci.refTableName() + "|" + raw;
                        String resolved = typeIdToLabel.get(lookupKey);
                        if (resolved == null && raw.equals("26340")) { // debug ciblé
                            log("[DEBUG REF] lookup '" + lookupKey + "' → NOT FOUND, typeIdToLabel size=" + typeIdToLabel.size());
                        }
                        keyVal = (resolved != null) ? resolved : raw;
                    } else {
                        // StringType, DoubleType, IntegerType, UniqueChoiceType, etc.
                        keyVal = raw;
                    }

                    if (!keyVal.isBlank()) {
                        if (!keyParts.isEmpty()) keyParts.append(", ");
                        keyParts.append(keyVal);
                    }
                }

                // Trouver le premier descriptif STRING non vide et non égal à la KEY
                String keyStr = keyParts.toString();
                String desc = null;
                for (int dc : descCols) {
                    if (dc >= cols.length) continue;
                    String v = clean(cols[dc]);
                    if (!v.isBlank() && !v.startsWith("#ZIP#") && !v.equals(keyStr)) {
                        desc = v;
                        break;
                    }
                }

                // Label final
                String label;
                if (!keyStr.isBlank() && desc != null) {
                    label = keyStr + ", " + desc;
                } else if (!keyStr.isBlank()) {
                    label = keyStr;
                } else if (desc != null) {
                    label = desc;
                } else {
                    continue; // Aucun label trouvable
                }

                guidToLabel.put(uid, label);
                try {
                    int id = Integer.parseInt(cols[0].trim());
                    typeIdToLabel.put(tableName + "|" + id, label);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[VpsLabelResolver] Erreur lecture " + tableName + " : " + e.getMessage());
        }
    }

    /** planningevent : col[1]=UID, col[12]=NAME. */
    private void loadPlanningEventTable(byte[] data) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 13) continue;
                try {
                    int    id    = Integer.parseInt(cols[0].trim());
                    String uid   = clean(cols[1]).toUpperCase(Locale.ROOT);
                    String name  = clean(cols[12]);
                    String label = name.isBlank() ? ("Evt. #" + id) : name;
                    if (!uid.isBlank()) guidToLabel.put(uid, label);
                    typeIdToLabel.put("planningevent|" + id, label);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[VpsLabelResolver] Erreur lecture planningevent : " + e.getMessage());
        }
    }


    // ── Parsing eventtreestruct.txt ───────────────────────────────────────────

    private void parseEventTreeStructs(byte[] data) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 2) continue;
                try {
                    int    treeId   = Integer.parseInt(cols[0].trim());
                    String treeName = clean(cols[1]);
                    // Trouver le payload XML (dernière colonne non triviale)
                    String b64 = "";
                    for (int i = cols.length - 1; i >= 2; i--) {
                        String c = clean(cols[i]);
                        if (c.length() > 100) { b64 = c; break; }
                    }
                    if (b64.isBlank()) continue;

                    byte[] xmlBytes  = Base64.getDecoder().decode(b64);
                    Document doc     = newDocumentBuilder().parse(
                        new InputSource(new StringReader(
                            new String(xmlBytes, StandardCharsets.UTF_8).replace("\r\n","\n"))));

                    boolean activated = "true".equals(
                        doc.getDocumentElement().getElementsByTagName("activated")
                        .item(0).getTextContent().trim());

                    List<EventTreeStruct.DimEntry> dims = new ArrayList<>();
                    Set<String> seen = new LinkedHashSet<>();
                    walkTreeNode(doc.getDocumentElement().getElementsByTagName("eventStructNode")
                        .item(0), dims, seen);

                    eventTrees.add(new EventTreeStruct(treeId, treeName, activated, dims));
                } catch (Exception e) {
                    System.err.println("[VpsLabelResolver] Erreur eventtreestruct: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[VpsLabelResolver] Erreur lecture eventtreestruct: " + e.getMessage());
        }
    }

    /** Parse récursivement un nœud EventStructNode. */
    private void walkTreeNode(org.w3c.dom.Node nodeEl, List<EventTreeStruct.DimEntry> dims,
                              Set<String> seen) {
        walkTreeNodeDepth(nodeEl, dims, seen, 0);
    }

    /**
     * Parcours récursif de l'arbre en transmettant la profondeur.
     * depth=0 → nœud racine = dimensions d'identification.
     * depth≥1 → nœuds enfants = sous-clés de planification.
     */
    private void walkTreeNodeDepth(org.w3c.dom.Node nodeEl, List<EventTreeStruct.DimEntry> dims,
                                   Set<String> seen, int depth) {
        if (nodeEl == null) return;
        Element el = (nodeEl instanceof Element e) ? e : null;
        if (el == null) return;

        // Lire les resourceModelStructs à CE niveau
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element child)) continue;
            if (!"resourceModelStructs".equals(child.getTagName())) continue;
            NodeList rmsNodes = child.getChildNodes();
            for (int j = 0; j < rmsNodes.getLength(); j++) {
                if (!(rmsNodes.item(j) instanceof Element rms)) continue;
                String rmId = "";
                NodeList rmModel = rms.getElementsByTagName("entityID");
                if (rmModel.getLength() > 0) rmId = rmModel.item(0).getTextContent().trim();
                String mandatory = "false";
                NodeList mandNodes = rms.getElementsByTagName("mandatory");
                if (mandNodes.getLength() > 0) mandatory = mandNodes.item(0).getTextContent().trim();
                if (!rmId.isBlank() && !"-1".equals(rmId) && !seen.contains(rmId)) {
                    seen.add(rmId);
                    // Stocker la profondeur : 0=identification, ≥1=planification
                    dims.add(new EventTreeStruct.DimEntry(rmId, "true".equals(mandatory), depth));
                }
            }
        }
        // Descendre dans childNode avec depth+1
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element child)) continue;
            if (!"childNode".equals(child.getTagName())) continue;
            if ("true".equals(child.getAttribute("isNull"))) continue;
            walkTreeNodeDepth(child, dims, seen, depth + 1);
        }
    }

    /** Construit le mapping rmId → colonne index dans planningevent.txt. */
    private void buildRmIdToPeColMap() {
        Map<String, Integer> peColIndex = tableColIndexes.get("planningevent");
        if (peColIndex == null) return;
        for (Map.Entry<String, Integer> e : peColIndex.entrySet()) {
            String colName = e.getKey();
            if (colName.startsWith("Resource")) {
                String rmId = colName.substring("Resource".length());
                rmIdToPeCol.put(rmId, e.getValue());
            }
        }
    }

    // ── Chargement planningevent enrichi ──────────────────────────────────────

    private static final DateTimeFormatter PE_DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Charge planningevent.txt en construisant des {@link PlanningEventInfo} enrichis.
     * Appelé en passe 4, après que tous les labels de ressources sont disponibles.
     */
    private void loadPlanningEventTableEnriched(byte[] data) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 8) continue;
                try {
                    int    id        = Integer.parseInt(cols[0].trim());
                    String uid       = clean(cols[1]).toUpperCase(Locale.ROOT);
                    String name      = cols.length > 12 ? clean(cols[12]) : "";
                    LocalDateTime begin = parseDateTime(cols.length > 3  ? cols[3]  : "");
                    LocalDateTime end   = parseDateTime(cols.length > 7  ? cols[7]  : "");

                    // Collecter les ressources renseignées : rmId → numericId
                    Map<String, String> filledRm = new LinkedHashMap<>();
                    Set<String>         filledRmIds = new LinkedHashSet<>();
                    for (Map.Entry<String, Integer> e : rmIdToPeCol.entrySet()) {
                        String rmId = e.getKey();
                        int    ci   = e.getValue();
                        if (ci >= cols.length) continue;
                        String v = clean(cols[ci]);
                        if (!v.isBlank() && !"-1".equals(v)) {
                            filledRm.put(rmId, v);
                            filledRmIds.add(rmId);
                        }
                    }

                    // Trouver l'arbre correspondant :
                    // toutes les mandatory présentes ET aucune dimension étrangère
                    EventTreeStruct bestTree = null;
                    int bestScore = -1;
                    for (EventTreeStruct tree : eventTrees) {
                        if (!tree.matches(filledRmIds)) continue;
                        int score = tree.matchScore(filledRmIds);
                        if (score > bestScore) { bestScore = score; bestTree = tree; }
                    }

                    // Résoudre les labels des ressources
                    Map<String, String> resourceLabels = new LinkedHashMap<>();
                    for (Map.Entry<String, String> e : filledRm.entrySet()) {
                        String rmId   = e.getKey();
                        String numId  = e.getValue();
                        String label  = typeIdToLabel.get("eventresource" + rmId + "|" + numId);
                        if (label != null) resourceLabels.put(rmId, label);
                    }

                    PlanningEventInfo info = new PlanningEventInfo(
                        id, uid, begin, end, name, bestTree, resourceLabels);

                    if (!uid.isBlank()) eventInfoByUid.put(uid, info);

                    // Mettre à jour le label dans guidToLabel et typeIdToLabel
                    String label = info.buildLabel();
                    if (!uid.isBlank())    guidToLabel.put(uid, label);
                    typeIdToLabel.put("planningevent|" + id, label);

                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[VpsLabelResolver] Erreur lecture planningevent enrichi: " + e.getMessage());
        }
    }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s.trim(), PE_DT_FMT); }
        catch (DateTimeParseException e) { return null; }
    }

    // ── Utilitaires ZIP ───────────────────────────────────────────────────────

    private static byte[] readEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        byte[] buf = new byte[16384];
        int len;
        while ((len = zis.read(buf)) > 0) baos.write(buf, 0, len);
        return baos.toByteArray();
    }

    private static String baseName(ZipEntry e) {
        String name = new File(e.getName()).getName().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    // ── Utilitaires XML ───────────────────────────────────────────────────────

    private static DocumentBuilder newDocumentBuilder() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return dbf.newDocumentBuilder();
    }

    /**
     * Recherche récursive dans tout le sous-arbre (comportement DOM standard).
     * À utiliser quand le tag cible ne peut exister qu'une seule fois en profondeur.
     */
    private static String firstText(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        String t = nl.item(0).getTextContent();
        return t != null ? t.trim() : "";
    }

    /**
     * Recherche uniquement dans les enfants DIRECTS de {@code parent}.
     * À utiliser quand le même tag existe à plusieurs niveaux de profondeur
     * (ex. {@code <name>} dans {@code <SQLT>} et dans ses {@code <SQLC>} enfants).
     */
    private static String directChildText(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child
                    && tag.equals(child.getTagName())) {
                String t = child.getTextContent();
                return t != null ? t.trim() : "";
            }
        }
        return "";
    }

    private static Element firstElement(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() > 0 ? (Element) nl.item(0) : null;
    }

    // ── Utilitaires String ────────────────────────────────────────────────────

    private static String clean(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
            s = s.substring(1, s.length() - 1).trim();
        return s;
    }
}
