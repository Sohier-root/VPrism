package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;

import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.reader.FilterConditionFormatter;
import com.stilog.prism.vpimodel.reader.RawFragmentIndexer;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.filter.VpiFilter;
import com.visualplanning.vpi.model.hierarchy.EventHierarchy;
import com.visualplanning.vpi.model.hierarchy.HierarchyNode;
import com.visualplanning.vpi.model.hierarchy.ResourceModelNodeEntry;

public class Hierarchies extends FileDatas {

	public Hierarchies(String filePath, String name) {
		super(filePath, name);
	}

	/*
	 * PARSE (via VPIReader)
	 */
	@Override
	protected List<Parameters> parseXml(Entity entity) {
		List<Parameters> paramList = new ArrayList<>();

		EventHierarchy hierarchy = findHierarchy(entity.getId());
		if (hierarchy == null) {
			System.out.println("Hiérarchie introuvable pour l'entité id=" + entity.getId());
			return paramList;
		}

		entity.setMergeable(true);
		entity.setReplaceable(true);
		entity.addUniqueAttributes(VPIConstants.XML_TAG_ID, String.valueOf(hierarchy.getId()));
		entity.addUniqueAttributes(VPIConstants.XML_TAG_UID, hierarchy.getUid());

		List<ResourceModelNodeEntry> entries = flatten(hierarchy.getRootNode());
		List<String> rawFragments = RawFragmentIndexer.fragmentsByOrder(entity.getAssociatedXml(), VPIConstants.XML_TAG_MODEL_STRUCT);

		for (int i = 0; i < entries.size(); i++) {
			ResourceModelNodeEntry entry = entries.get(i);
			String resourceName = entry.getResourceModel().getDisplayValue();

			Parameters param = new Parameters(resourceName);
			if (i < rawFragments.size()) {
				param.setInitialXml(rawFragments.get(i));
				param.setAssociatedXml(rawFragments.get(i));
			}
			param.addAttributes(VPIConstants.PARAMETER_CONDITIONS, conditionsText(entry));
			param.addAttributes(VPIConstants.PARAMETER_MANDATORY, entry.getMandatory().getDisplayValue());
			param.setMergeable(true);
			param.setReplaceable(true);
			paramList.add(param);
		}

		return paramList;
	}

	private EventHierarchy findHierarchy(int id) {
		for (EventHierarchy h : this.planning.getHierarchies()) {
			if (h.getId() == id)
				return h;
		}
		return null;
	}

	/** Aplatit la chaîne récursive de niveaux en une liste, dans l'ordre du document. */
	private List<ResourceModelNodeEntry> flatten(HierarchyNode node) {
		List<ResourceModelNodeEntry> all = new ArrayList<>();
		HierarchyNode current = node;
		while (current != null) {
			all.addAll(current.resourceModelEntries());
			current = current.child().orElse(null);
		}
		return all;
	}

	private String conditionsText(ResourceModelNodeEntry entry) {
		VpiFilter filter = entry.getFilter() != null ? entry.getFilter().getValue() : null;
		return filter != null ? FilterConditionFormatter.format(filter.getRootCondition()) : "";
	}
}
