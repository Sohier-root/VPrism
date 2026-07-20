package com.stilog.prism.vpimodel.reader;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.visualplanning.vpi.model.VpiPlanning;
import com.visualplanning.vpi.model.dimension.Dimension;
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

	public static String formatAll(List<FilterCondition> conditions) {
		return conditions.stream().map(FilterConditionFormatter::format).collect(Collectors.joining(" ; "));
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
