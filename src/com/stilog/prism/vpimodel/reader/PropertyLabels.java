package com.stilog.prism.vpimodel.reader;

import java.util.Set;

import com.visualplanning.vpi.model.dimension.HeadingType;

/**
 * Libellés français affichés pour chaque {@link HeadingType} VPIReader, et
 * détection des types "calculés" (non éditables), en remplacement de
 * l'ancien {@code resolveTypeLabel(String typeClass)} qui lisait le nom de
 * classe VP directement dans le XML.
 */
public class PropertyLabels {

	private static final Set<HeadingType> COMPUTED_TYPES = Set.of(
			HeadingType.CONTROL_TOTAL,
			HeadingType.OPERATION,
			HeadingType.TEXT_CONCAT,
			HeadingType.COMPUTED_DATE,
			HeadingType.EVENT_VALUE,
			HeadingType.EVENT_SUMMARY);

	private PropertyLabels() {
	}

	public static String label(HeadingType type) {
		if (type == null)
			return "?";
		return switch (type) {
			case TEXT -> "Texte";
			case MULTI_LINE -> "Texte multiligne";
			case BOOLEAN -> "Booléen";
			case COUNTER -> "Compteur";
			case NUMERIC -> "Nombre";
			case UNIQUE_CHOICE -> "Liste (unique)";
			case MULTI_CHOICE -> "Liste (multiple)";
			case DATE_TIME -> "Date/Heure";
			case ATTACHMENT -> "Pièce jointe";
			case DOCUMENT -> "Document";
			case BAR_CODE -> "Code-barres";
			case RESOURCE_REFERENCE -> "Référence dimension";
			case EVENT_SUMMARY -> "Résumé d'événement";
			case TEXT_CONCAT -> "Calculé (texte)";
			case CONTROL_TOTAL -> "Calculé (contrôle)";
			case OPERATION -> "Calculé (nombre)";
			case COMPUTED_DATE -> "Date calculée";
			case EVENT_VALUE -> "Valeur événement";
			case GEO_LOCATION -> "Géolocalisation";
			case BLUEPRINT -> "Plan";
			case DISTANCE -> "Distance";
		};
	}

	/** Champs calculés/dérivés : non mergeable/replaceable, à l'image de l'ancien COMPUTED_TYPES. */
	public static boolean isComputed(HeadingType type) {
		return type != null && COMPUTED_TYPES.contains(type);
	}
}
