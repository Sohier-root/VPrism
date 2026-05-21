package com.stilog.prism.analyzevpi.model.history;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @deprecated Remplacé par {@link TrackerRef} + {@link TrackerIndex}.
 *             Conservé temporairement pour compatibilité avec le code existant.
 *             À supprimer dans une prochaine version.
 *
 * <p>Avec la nouvelle architecture, les métadonnées sont dans {@link TrackerRef}
 * et les {@link PropertyChange} sont chargées à la demande via
 * {@link HistoryTrackerParser#loadChanges}.
 */
@Deprecated
public class HistoryTrackerEntry {

    private final long          trackerId;
    private final String        guid;
    private final String        entityType;
    private final LocalDateTime date;
    private final String        user;
    private final List<PropertyChange> changes;

    public HistoryTrackerEntry(long trackerId, String guid, String entityType,
                               LocalDateTime date, String user,
                               List<PropertyChange> changes) {
        this.trackerId  = trackerId;
        this.guid       = guid;
        this.entityType = entityType;
        this.date       = date;
        this.user       = user;
        this.changes    = changes != null ? changes : List.of();
    }

    public long          getTrackerId()  { return trackerId; }
    public String        getGuid()       { return guid; }
    public String        getEntityType() { return entityType; }
    public LocalDateTime getDate()       { return date; }
    public String        getUser()       { return user; }
    public List<PropertyChange> getChanges() { return changes; }

    public String getChangeSummary() {
        if (changes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (PropertyChange c : changes) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c.getPropertyName());
        }
        return sb.toString();
    }
}
