package com.stilog.prism.analyzevpi.model.history;

import java.util.*;

/**
 * Index léger en mémoire : GUID → liste de {@link TrackerRef}.
 *
 * <p>Chaque {@link TrackerRef} stocke uniquement les métadonnées d'une ligne
 * de {@code historytracker.txt} (id, date, user, type) plus la position
 * du payload base64 dans le fichier décompressé sur disque. Le XML des
 * {@link PropertyChange} n'est <b>jamais</b> décodé ni conservé en mémoire
 * à ce stade — il sera lu à la demande via
 * {@link HistoryTrackerParser#loadChanges}.
 *
 * <h2>Empreinte mémoire</h2>
 * Un {@link TrackerRef} occupe environ 80–120 bytes (champs primitifs + String
 * internés). Pour 5 millions de lignes, l'index représente ~500 MB au pire,
 * mais en pratique beaucoup moins car les Strings (guid, user, type) sont
 * partagées via {@link String#intern()}.
 */
public class TrackerIndex {

    /** GUID (uppercase) → liste ordonnée chronologiquement. */
    private final Map<String, List<TrackerRef>> index = new HashMap<>(64 * 1024);

    /** Nombre total de références indexées. */
    private int totalRefs = 0;

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Ajoute une référence dans l'index.
     * Appelé par {@link HistoryTrackerParser} pendant le scan streaming.
     */
    void add(String guid, TrackerRef ref) {
        index.computeIfAbsent(guid, k -> new ArrayList<>(4)).add(ref);
        totalRefs++;
    }

    /**
     * Trie toutes les listes par date croissante.
     * Appelé une seule fois à la fin du scan.
     */
    void sort() {
        for (List<TrackerRef> refs : index.values()) {
            if (refs.size() > 1) {
                refs.sort(Comparator.comparing(
                    r -> r.date() != null ? r.date() : java.time.LocalDateTime.MIN));
            }
        }
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    /**
     * Retourne les références pour un GUID donné, ou liste vide si inconnu.
     *
     * @param guid GUID brut (la normalisation uppercase est faite ici)
     */
    public List<TrackerRef> get(String guid) {
        if (guid == null || guid.isBlank()) return List.of();
        List<TrackerRef> refs = index.get(guid.toUpperCase(java.util.Locale.ROOT));
        return refs != null ? refs : List.of();
    }

    public int size()      { return index.size(); }
    public int totalRefs() { return totalRefs; }
    public boolean isEmpty() { return index.isEmpty(); }
}
