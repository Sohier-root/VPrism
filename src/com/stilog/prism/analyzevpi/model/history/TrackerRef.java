package com.stilog.prism.analyzevpi.model.history;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Référence légère vers une ligne de {@code historytracker.txt}.
 *
 * <p>Stocke les métadonnées (colonnes 0–4) et le payload base64 brut
 * (colonne 5) directement en mémoire. Le décodage XML est différé jusqu'au
 * clic utilisateur via {@link HistoryTrackerParser#loadChanges}.
 *
 * <p>Les Strings {@code guid}, {@code entityType} et {@code user} sont
 * internées ({@link String#intern()}) pour minimiser la duplication mémoire
 * sur les grands fichiers où les mêmes valeurs reviennent des milliers de fois.
 *
 * @param trackerId  identifiant numérique de la ligne (col 0)
 * @param guid       GUID de l'entité (col 1, uppercase, interné)
 * @param entityType type de l'entité (col 2, interné)
 * @param date       date de la modification (col 3)
 * @param user       utilisateur (col 4, interné)
 * @param payload    payload base64 brut (col 5, sans guillemets)
 */
public record TrackerRef(
    long          trackerId,
    String        guid,
    String        entityType,
    LocalDateTime date,
    String        user,
    String        payload
) {
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Résumé court pour affichage. Ex : "2025-11-17 17:52:37 — admin"
     */
    public String summary() {
        String d = date != null ? date.format(DT_FMT) : "?";
        return d + " — " + (user != null ? user : "?");
    }
}
