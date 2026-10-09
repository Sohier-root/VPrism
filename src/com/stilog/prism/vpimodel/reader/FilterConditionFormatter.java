package com.stilog.prism.vpimodel.reader;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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
import com.visualplanning.vpi.model.filter.FilterSet;
import com.visualplanning.vpi.model.hierarchy.EventHierarchy;

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
			return c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName(), c.operator());
		if (condition instanceof FilterCondition.ResourceAttributeCondition c)
			return c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName(), c.operator());
		if (condition instanceof FilterCondition.HistoryCondition c)
			return "Historique." + c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName(), c.operator());
		if (condition instanceof FilterCondition.FormAttributeCondition c)
			return c.attribute().title() + " " + c.operator() + " " + formatValue(c.value(), c.dynamic(), c.variableName(), c.operator());
		if (condition instanceof FilterCondition.EventFilterCondition c)
			return c.conditionType() + " " + c.operator() + (c.filterValue() != null ? " " + c.filterValue() : "");

		return condition.toString();
	}

	/** Un ID de filtre référencé (ex. INFILTER) ou une liste d'ID de ressources (ex. IN) : chiffres, virgules, espaces. */
	private static final Pattern SIMPLE_NUMERIC_LIST = Pattern.compile("\\d+(\\s*,\\s*\\d+)*");

	private static String formatValue(String value, boolean dynamic, String variableName, String operator) {
		return formatValue(value, dynamic, variableName, operator, null);
	}

	/**
	 * Variante avec résolution du nom des filtres référencés par INFILTER/NOTINFILTER et de la
	 * hiérarchie référencée par ISA (quand {@code planning} est fourni) — voir
	 * {@link #isEmbeddedSubFilterOperator} et {@link #formatHierarchyLevelValue}.
	 */
	private static String formatValue(String value, boolean dynamic, String variableName, String operator, VpiPlanning planning) {
		if (dynamic)
			return "[" + variableName + "]";
		if (value == null)
			return "";
		if (isEmbeddedSubFilterOperator(operator)) {
			String trimmed = value.trim();
			if (!SIMPLE_NUMERIC_LIST.matcher(trimmed).matches())
				return "Filtre personnalisé";
			return resolveFilterNames(trimmed, planning);
		}
		if ("ISA".equals(operator))
			return formatHierarchyLevelValue(value, planning);
		return value;
	}

	/**
	 * Valeur de l'opérateur ISA (ex. sur l'attribut événement "Hierarchy") : VPIReader encode
	 * {@code <idHiérarchie>:<niveau>} (voir FilterParser.hierarchyLevelValue côté VPIReader,
	 * qui sépare ces deux champs plutôt que de les concaténer en un nombre illisible comme
	 * "-1120"). On résout ici le nom de la hiérarchie via {@code planning.getHierarchies()}.
	 */
	private static String formatHierarchyLevelValue(String value, VpiPlanning planning) {
		int sep = value.indexOf(':');
		if (sep < 0) {
			VpsLabelResolver.logDebug("[FilterConditionFormatter] ISA value='" + value
					+ "' : pas de ':' trouvé (jar vpi-reader pas à jour ? devrait être 'idHiérarchie:niveau'), repli sur brut");
			return value;
		}
		String idPart = value.substring(0, sep).trim();
		String levelPart = value.substring(sep + 1).trim();
		int hierarchyId;
		try {
			hierarchyId = Integer.parseInt(idPart);
		} catch (NumberFormatException e) {
			VpsLabelResolver.logDebug("[FilterConditionFormatter] ISA value='" + value
					+ "' : partie ID non numérique ('" + idPart + "'), repli sur brut");
			return value;
		}
		String name = resolveHierarchyName(hierarchyId, planning);
		if (name == null) {
			VpsLabelResolver.logDebug("[FilterConditionFormatter] ISA hierarchyId=" + hierarchyId
					+ " niveau=" + levelPart + " planning=" + (planning == null ? "absent" : "présent")
					+ " : nom de hiérarchie non résolu, repli sur ID brut");
		}
		return (name != null ? name : idPart) + ", niveau " + levelPart;
	}

	private static String resolveHierarchyName(int hierarchyId, VpiPlanning planning) {
		if (planning == null)
			return null;
		for (EventHierarchy h : planning.getHierarchies()) {
			if (h.getId() != hierarchyId)
				continue;
			String name = h.getName() != null ? h.getName().getValue() : null;
			return (name != null && !name.isBlank()) ? name : null;
		}
		return null;
	}

	/**
	 * INFILTER/NOTINFILTER peuvent référencer un filtre existant par ID (valeur simple, ex.
	 * "53", résolue en son nom via {@link #resolveFilterNames}) OU embarquer un filtre entier
	 * ad-hoc (anonyme, potentiellement récursif) dans le XML de la condition — VPIReader ne
	 * distingue pas encore les deux cas et aplatit ce second cas en texte brut illisible (UID,
	 * booléens et mots-clés concaténés sans séparateur). En attendant une résolution complète
	 * (parsing récursif du sous-filtre côté VPIReader), on détecte ce cas via la forme de la
	 * valeur et on affiche un texte neutre plutôt que ce charabia.
	 */
	private static boolean isEmbeddedSubFilterOperator(String operator) {
		return "INFILTER".equals(operator) || "NOTINFILTER".equals(operator);
	}

	/** Résout chaque ID de filtre (séparés par ", ") en son nom, via les filtres settings de {@code planning}. */
	private static String resolveFilterNames(String idList, VpiPlanning planning) {
		if (planning == null || planning.getFilterSet() == null)
			return idList;
		FilterSet filterSet = planning.getFilterSet();
		List<String> resolved = new ArrayList<>();
		for (String token : idList.split(",\\s*")) {
			String name = resolveFilterName(token.trim(), filterSet);
			resolved.add(name != null ? name : token.trim());
		}
		return String.join(", ", resolved);
	}

	private static String resolveFilterName(String token, FilterSet filterSet) {
		try {
			int id = Integer.parseInt(token);
			return filterSet.resourceFilterById(id)
					.or(() -> filterSet.eventFilterById(id))
					.map(f -> f.getName().getValue())
					.filter(n -> n != null && !n.isBlank())
					.orElse(null);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Convertit directement un arbre {@link FilterCondition} VPIReader vers le modèle
	 * d'affichage de VPrism ({@link FilterGroupNode}/{@link FilterLeafNode}), sans repasser
	 * par du texte. Sans résolveur (utilisé par la comparaison sémantique, qui compare des
	 * ID et non des libellés) : équivalent à {@code toFilterGroupNode(condition, null)}.
	 *
	 * Limitation connue : INFILTER avec un sous-filtre imbriqué (par opposition à un simple ID
	 * de filtre référencé) reste affiché avec un texte neutre ("Filtre personnalisé") plutôt que
	 * résolu, VPIReader n'exposant pas (encore) cette donnée de façon structurée et récursive.
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
			return new FilterLeafNode(c.attribute().title(), c.operator(), formatValue(c.value(), c.dynamic(), c.variableName(), c.operator(), planning), c.dynamic());
		if (condition instanceof FilterCondition.ResourceAttributeCondition c)
			return new FilterLeafNode(c.attribute().title(), c.operator(), formatResourceValue(c, resolver, planning), c.dynamic());
		if (condition instanceof FilterCondition.HistoryCondition c)
			return new FilterLeafNode("Historique." + c.attribute().title(), c.operator(), formatValue(c.value(), c.dynamic(), c.variableName(), c.operator(), planning), c.dynamic());
		if (condition instanceof FilterCondition.FormAttributeCondition c)
			return new FilterLeafNode(c.attribute().title(), c.operator(), formatValue(c.value(), c.dynamic(), c.variableName(), c.operator(), planning), c.dynamic());
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
		String raw = formatValue(c.value(), c.dynamic(), c.variableName(), c.operator(), planning);
		if (c.dynamic() || resolver == null || c.value() == null || isEmbeddedSubFilterOperator(c.operator()))
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
