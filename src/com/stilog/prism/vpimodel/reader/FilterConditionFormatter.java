package com.stilog.prism.vpimodel.reader;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.stilog.prism.analyzevpi.model.history.VpsLabelResolver;
import com.stilog.prism.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.prism.vpimodel.objects.filter.FilterLeafNode;
import com.stilog.prism.vpimodel.objects.filter.FilterNode;
import com.visualplanning.vpi.model.VpiPlanning;
import com.visualplanning.vpi.model.dimension.Dimension;
import com.visualplanning.vpi.model.dimension.Heading;
import com.visualplanning.vpi.model.dimension.heading.HeadingResourceReference;
import com.visualplanning.vpi.model.filter.FilterCondition;

/**
 * Rend un arbre {@link FilterCondition} VPIReader en texte lisible, en
 * remplacement du XML brut du nœud {@code <filterCondition>} affiché par
 * l'ancien parsing DOM dans l'attribut {@code PARAMETER_CONDITIONS}.
 */
public class FilterConditionFormatter {

	private FilterConditionFormatter() {
	}

	public static String format(FilterCondition condition) {
		if (condition == null)
			return "";

		if (condition instanceof FilterCondition.LogicGroup g) {
			String joined = g.conditions().stream()
					.map(FilterConditionFormatter::format)
					.collect(Collectors.joining(" " + g.operator() + " "));
			return "(" + joined + ")";
		}
		if (condition instanceof FilterCondition.EventAttributeCondition c)
			return c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName());
		if (condition instanceof FilterCondition.ResourceAttributeCondition c)
			return c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName());
		if (condition instanceof FilterCondition.HistoryCondition c)
			return "Historique." + c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName());
		if (condition instanceof FilterCondition.FormAttributeCondition c)
			return c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName());
		if (condition instanceof FilterCondition.EventFilterCondition c)
			return c.conditionType() + " " + c.operator() + (c.filterValue() != null ? " " + c.filterValue() : "");

		return condition.toString();
	}

	private static String formatValue(String value, boolean dynamic, String variableName) {
		if (dynamic)
			return "[" + variableName + "]";
		return value != null ? value : "";
	}

	/**
	 * Convertit directement un arbre {@link FilterCondition} VPIReader vers le modèle
	 * d'affichage de VPrism ({@link FilterGroupNode}/{@link FilterLeafNode}), sans repasser
	 * par du texte. Sans résolveur (utilisé par la comparaison sémantique, qui compare des
	 * ID et non des libellés) : équivalent à {@code toFilterGroupNode(condition, null)}.
	 *
	 * Limitation connue : INFILTER (référence à un autre filtre) et ISA (référence à une
	 * hiérarchie) restent en texte brut non résolu, VPIReader n'exposant pas encore cette
	 * donnée de façon structurée.
	 */
	public static FilterGroupNode toFilterGroupNode(FilterCondition condition) {
		return toFilterGroupNode(condition, null, null);
	}

	/**
	 * Variante avec résolution des ID de ressources en noms lisibles pour les conditions
	 * "liste de valeurs" (IN sur plusieurs ressources) — utilisée par le dialogue de détail
	 * de filtre. {@code resolver} peut être null (résolution dégradée sur ID brut).
	 */
	public static FilterGroupNode toFilterGroupNode(FilterCondition condition, VpsLabelResolver resolver) {
		return toFilterGroupNode(condition, resolver, null);
	}

	/**
	 * Variante complète : {@code planning} permet en plus de retrouver la dimension
	 * réellement référencée par un attribut "référence de ressource" porté par une autre
	 * dimension (ex. l'attribut "Plant" du Work Center référence la dimension Plant) —
	 * voir {@link #resolveTargetDimensionId}. Sans {@code planning}, on retombe sur
	 * {@code resourceModelEntityId()} tel quel.
	 */
	public static FilterGroupNode toFilterGroupNode(FilterCondition condition, VpsLabelResolver resolver, VpiPlanning planning) {
		if (condition instanceof FilterCondition.LogicGroup g) {
			FilterGroupNode node = new FilterGroupNode(g.operator());
			for (FilterCondition child : g.conditions())
				node.addChild(toFilterNode(child, resolver, planning));
			return node;
		}
		FilterGroupNode wrapper = new FilterGroupNode("AND");
		wrapper.addChild(toFilterNode(condition, resolver, planning));
		return wrapper;
	}

	private static FilterNode toFilterNode(FilterCondition condition, VpsLabelResolver resolver, VpiPlanning planning) {
		if (condition instanceof FilterCondition.LogicGroup g)
			return toFilterGroupNode(g, resolver, planning);
		if (condition instanceof FilterCondition.EventAttributeCondition c)
			return new FilterLeafNode(c.attribute().title(), c.operator(), formatValue(c.value(), c.dynamic(), c.variableName()), c.dynamic());
		if (condition instanceof FilterCondition.ResourceAttributeCondition c)
			return new FilterLeafNode(c.attribute().title(), c.operator(), formatResourceValue(c, resolver, planning), c.dynamic());
		if (condition instanceof FilterCondition.HistoryCondition c)
			return new FilterLeafNode("Historique." + c.attribute().title(), c.operator(), formatValue(c.value(), c.dynamic(), c.variableName()), c.dynamic());
		if (condition instanceof FilterCondition.FormAttributeCondition c)
			return new FilterLeafNode(c.attribute().title(), c.operator(), formatValue(c.value(), c.dynamic(), c.variableName()), c.dynamic());
		if (condition instanceof FilterCondition.EventFilterCondition c)
			return new FilterLeafNode(c.conditionType().name(), c.operator(), c.filterValue() != null ? c.filterValue() : "", false);
		return new FilterLeafNode(condition.toString(), "", "", false);
	}

	/**
	 * Valeur d'une condition sur attribut ressource, avec résolution des ID de ressources
	 * en noms lisibles quand un résolveur est fourni : la valeur brute est une liste d'ID
	 * séparés par ", " (ex. "3, 1") dans la dimension cible (voir {@link #resolveTargetDimensionId}).
	 * Repli sur l'ID brut si le résolveur est absent, ou pour tout ID non résolu (ressource
	 * supprimée depuis, dimension non chargée…).
	 */
	private static String formatResourceValue(FilterCondition.ResourceAttributeCondition c, VpsLabelResolver resolver, VpiPlanning planning) {
		String raw = formatValue(c.value(), c.dynamic(), c.variableName());
		if (c.dynamic() || resolver == null || c.value() == null)
			return raw;

		int targetDimId = resolveTargetDimensionId(c, planning);
		if (targetDimId == -1) {
			VpsLabelResolver.logDebug("[FilterConditionFormatter] attribut='" + c.attribute().title()
					+ "' operator='" + c.operator() + "' value='" + c.value()
					+ "' : aucune dimension cible trouvée, repli sur brut");
			return raw;
		}

		String tableName = "eventresource" + targetDimId;
		List<String> resolved = new ArrayList<>();
		boolean anyResolved = false;
		for (String token : c.value().split(",\\s*")) {
			String label = null;
			try {
				label = resolver.resolveTypeAndId(tableName, Integer.parseInt(token.trim()));
			} catch (NumberFormatException ignored) {
				// jeton non numérique : on le garde tel quel
			}
			if (label == null || label.isBlank()) {
				VpsLabelResolver.logDebug("[FilterConditionFormatter] attribut='" + c.attribute().title()
						+ "' operator='" + c.operator() + "' table='" + tableName + "' id='" + token.trim()
						+ "' : non résolu (dimension cible=" + targetDimId + ")");
			}
			resolved.add(label != null && !label.isBlank() ? label : token.trim());
			anyResolved |= (label != null && !label.isBlank());
		}
		return anyResolved ? String.join(", ", resolved) : raw;
	}

	/**
	 * Détermine la dimension réellement référencée par les valeurs de cette condition.
	 *
	 * <p>{@code resourceModelEntityId()} (lu depuis {@code <resourceModel><entityID>} dans le
	 * XML de la condition) ne désigne PAS la dimension cible lorsque l'attribut "référence de
	 * ressource" est porté par une AUTRE dimension que celle filtrée (ex. l'attribut "Plant"
	 * du Work Center référence la dimension Plant) : il vaut alors l'ID de la dimension
	 * PROPRIÉTAIRE de l'attribut (Work Center), identique à {@code attribute().resourceModelId()}.
	 * La vraie cible doit être retrouvée via la définition du Heading "ResourceReference"
	 * correspondant sur la dimension propriétaire.
	 *
	 * <p>Repli sur {@code resourceModelEntityId()} si {@code planning} est absent, si la
	 * dimension propriétaire ou le heading ne sont pas trouvés, ou si l'attribut n'a pas de
	 * dimension propriétaire (ex. condition portée directement par un événement).
	 */
	private static int resolveTargetDimensionId(FilterCondition.ResourceAttributeCondition c, VpiPlanning planning) {
		int ownerDimId = c.attribute().resourceModelId();
		if (planning != null && ownerDimId != -1) {
			for (Dimension dim : planning.getDimensions()) {
				if (dim.getId() != ownerDimId)
					continue;
				for (Heading h : dim.getHeadings()) {
					if (h.getId() == c.attribute().attributeId() && h instanceof HeadingResourceReference hrr) {
						int refId = hrr.getReferencedDimension().getEntityId();
						if (refId != -1)
							return refId;
					}
				}
				break;
			}
		}
		return c.resourceModelEntityId();
	}

	/**
	 * Noms des dimensions référencées par les conditions sur attribut ressource
	 * de cet arbre (utilisé par le diagramme de dépendances pour relier un
	 * filtre/niveau de hiérarchie aux dimensions qu'il consulte).
	 */
	public static Set<String> referencedDimensionNames(FilterCondition condition, VpiPlanning planning) {
		Set<String> names = new LinkedHashSet<>();
		collectReferencedDimensionIds(condition, names, planning);
		return names;
	}

	private static void collectReferencedDimensionIds(FilterCondition condition, Set<String> names, VpiPlanning planning) {
		if (condition == null)
			return;

		if (condition instanceof FilterCondition.LogicGroup g) {
			for (FilterCondition child : g.conditions())
				collectReferencedDimensionIds(child, names, planning);
			return;
		}
		if (condition instanceof FilterCondition.ResourceAttributeCondition c && c.resourceModelEntityId() != -1) {
			for (Dimension dim : planning.getDimensions()) {
				if (dim.getId() == c.resourceModelEntityId()) {
					names.add(dim.getName().getDisplayValue());
					break;
				}
			}
		}
	}
}
