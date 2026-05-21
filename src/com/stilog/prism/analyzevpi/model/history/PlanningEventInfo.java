package com.stilog.prism.analyzevpi.model.history;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Informations enrichies d'un {@code PlanningEvent}, construites par
 * {@link VpsLabelResolver} à partir de {@code planningevent.txt}.
 *
 * <p>Contient les données nécessaires pour identifier et afficher un événement
 * dans l'onglet historique sans avoir à relire le fichier à chaque fois.
 *
 * @param id            identifiant numérique de l'événement
 * @param uid           UID (GUID) de l'événement
 * @param beginDate     date de début
 * @param endDate       date de fin
 * @param name          nom de l'événement (souvent vide dans VP)
 * @param treeStruct    arbre de création le plus compatible (peut être null)
 * @param resourceLabels map rmId → label résolu de la ressource affectée
 */
public record PlanningEventInfo(
    int                id,
    String             uid,
    LocalDateTime      beginDate,
    LocalDateTime      endDate,
    String             name,
    EventTreeStruct    treeStruct,
    Map<String, String> resourceLabels
) {
    private static final java.time.format.DateTimeFormatter DATE_FMT =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final java.time.format.DateTimeFormatter FULL_FMT =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Construit le label d'affichage de l'événement.
     *
     * <p>Priorité :
     * <ol>
     *   <li>Si {@code name} non vide → nom + dates</li>
     *   <li>Si arbre connu → dimensions obligatoires résolues + dates</li>
     *   <li>Fallback → toutes les dimensions renseignées + dates</li>
     * </ol>
     */
    public String buildLabel() {
        StringBuilder sb = new StringBuilder();

        if (name != null && !name.isBlank()) {
            sb.append(name);
        } else if (treeStruct != null && !treeStruct.getIdentificationDims().isEmpty()) {
            // Dimensions obligatoires de l'arbre dans l'ordre
            for (EventTreeStruct.DimEntry d : treeStruct.getIdentificationDims()) {
                String label = resourceLabels.get(d.rmId());
                if (label != null && !label.isBlank()) {
                    if (!sb.isEmpty()) sb.append(" | ");
                    sb.append(label);
                }
            }
        } else {
            // Fallback : toutes les ressources renseignées
            for (String label : resourceLabels.values()) {
                if (label != null && !label.isBlank()) {
                    if (!sb.isEmpty()) sb.append(" | ");
                    sb.append(label);
                }
            }
        }

        // Ajouter les dates
        if (beginDate != null) {
            if (!sb.isEmpty()) sb.append("  ");
            sb.append(beginDate.format(DATE_FMT));
            if (endDate != null && !endDate.toLocalDate().equals(beginDate.toLocalDate())) {
                sb.append("→").append(endDate.format(DATE_FMT));
            }
        }

        return sb.isEmpty() ? ("Evt. #" + id) : sb.toString();
    }

    /** Nom de l'arbre de création (type d'événement), ou "PlanningEvent" si inconnu. */
    public String treeLabel() {
        return treeStruct != null ? treeStruct.getName() : "PlanningEvent";
    }

    /** Durée en jours (approximative, peut être nulle si dates absentes). */
    public Integer durationDays() {
        if (beginDate == null || endDate == null) return null;
        return (int) java.time.Duration.between(beginDate, endDate).toDays();
    }
}
