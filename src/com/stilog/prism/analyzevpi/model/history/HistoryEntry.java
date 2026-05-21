package com.stilog.prism.analyzevpi.model.history;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une ligne du fichier {@code history.txt}.
 *
 * <p>Format CSV (séparateur {@code ;}) :
 * <pre>
 *   id ; dateCreation ; userCreation ; ; "" ; type ; dateModif ; userModif ; guid
 * </pre>
 *
 * <p>Les modifications détaillées ({@link PropertyChange}) ne sont <b>plus</b>
 * stockées ici : elles sont chargées à la demande via
 * {@link HistoryTrackerParser#loadChanges} lorsque l'utilisateur sélectionne
 * une ligne dans l'interface. Seules les {@link TrackerRef} (métadonnées
 * légères) sont rattachées à l'entrée.
 */
public class HistoryEntry {

    private final int           id;
    private final LocalDateTime dateCreation;
    private final String        userCreation;
    private final String        type;
    private final LocalDateTime dateModification;
    private final String        userModification;
    private final String        guid;

    /** Nom lisible résolu depuis le VPS (null si non résolu). */
    private String resolvedLabel = null;

    /**
     * Nom de la dimension en lieu et place du type technique (ex. "OUTSOURCE"
     * au lieu de "EventResource17"). Null si non résolu ou type non-EventResource.
     */
    private String displayType = null;

    /**
     * Informations enrichies si l'entité est un PlanningEvent.
     * Null pour tous les autres types.
     */
    private PlanningEventInfo eventInfo = null;

    /**
     * Références légères vers les lignes de {@code historytracker.txt}.
     * Chaque {@link TrackerRef} contient uniquement les métadonnées + offset
     * fichier ; le décodage XML est différé.
     */
    private final List<TrackerRef> trackerRefs = new ArrayList<>(4);

    public HistoryEntry(int id,
                        LocalDateTime dateCreation,
                        String userCreation,
                        String type,
                        LocalDateTime dateModification,
                        String userModification,
                        String guid) {
        this.id               = id;
        this.dateCreation     = dateCreation;
        this.userCreation     = userCreation;
        this.type             = type;
        this.dateModification = dateModification;
        this.userModification = userModification;
        this.guid             = guid;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int           getId()               { return id; }
    public LocalDateTime getDateCreation()     { return dateCreation; }
    public String        getUserCreation()     { return userCreation; }
    public String        getType()             { return type; }
    public LocalDateTime getDateModification() { return dateModification; }
    public String        getUserModification() { return userModification; }
    public String        getGuid()             { return guid; }

    // ── TrackerRefs ───────────────────────────────────────────────────────────

    public List<TrackerRef> getTrackerRefs()           { return trackerRefs; }
    public void addTrackerRef(TrackerRef ref)           { trackerRefs.add(ref); }
    public int  getTrackerCount()                       { return trackerRefs.size(); }

    // ── Label résolu ──────────────────────────────────────────────────────────

    public String  getResolvedLabel()         { return resolvedLabel; }
    public void    setResolvedLabel(String l) { this.resolvedLabel = l; }

    public String  getDisplayType()           { return displayType != null ? displayType : type; }
    public void    setDisplayType(String t)   { this.displayType = t; }

    public PlanningEventInfo getEventInfo()                 { return eventInfo; }
    public void              setEventInfo(PlanningEventInfo e) { this.eventInfo = e; }
    public boolean           isPlanningEvent()              { return eventInfo != null; }

    /** Vrai si le label a été résolu (entité encore présente dans le VPS). */
    public boolean isResolved() {
        return resolvedLabel != null && !resolvedLabel.isBlank();
    }

    /**
     * Meilleur libellé disponible : nom résolu si dispo, sinon "Type #id".
     */
    public String getDisplayLabel() {
        if (isResolved()) return resolvedLabel;
        return type + " #" + id;
    }

    @Override
    public String toString() {
        return "[" + type + " #" + id + "] " + guid;
    }
}
