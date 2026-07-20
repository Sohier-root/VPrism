package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.stilog.prism.comparevpi.model.GeneralCorrespondance;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.reader.FilterConditionFormatter;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.filter.VpiFilter;

public class Filter extends FileDatas{

	/**
	 * Clé de correspondance utilisée dans GeneralCorrespondance.
	 * Différente selon le type de filtre (ressource ou événement),
	 * définie par la sous-classe via setFilterCorrespondanceKey().
	 */
	private String filterCorrespondanceKey = VPIConstants.XML_TAG_FILTER_RESOURCE;

	public Filter(String filePath, String name) {
		super(filePath, name);
	}

	public void setFilterCorrespondanceKey(String key) {
		this.filterCorrespondanceKey = key;
	}

	private boolean isEventFilter() {
		return VPIConstants.XML_TAG_FILTER_EVENT.equals(filterCorrespondanceKey);
	}

	/*
	 * PARSE (via VPIReader)
	 */
	@Override
	protected List<Parameters> parseXml(Entity entity) {

		List<Parameters> paramList = new ArrayList<>();

		Optional<VpiFilter> found = isEventFilter()
				? this.planning.getFilterSet().eventFilterById(entity.getId())
				: this.planning.getFilterSet().resourceFilterById(entity.getId());

		if (found.isEmpty()) {
			System.out.println("Filtre introuvable pour l'entité id=" + entity.getId());
			return paramList;
		}
		VpiFilter filter = found.get();

		entity.addUniqueAttributes(VPIConstants.XML_TAG_ID, String.valueOf(filter.getId()));
		entity.addUniqueAttributes(VPIConstants.XML_TAG_UID, filter.getUid());
		entity.setReplaceable(true);
		entity.setMergeable(true);

		/*
		 * Enregistrement dans GeneralCorrespondance pour résolution INFILTER
		 * Clé : filterCorrespondanceKey (ressource ou événement), valeur : id → nom
		 */
		GeneralCorrespondance.getInstance().addCorrespondance(
			filterCorrespondanceKey, String.valueOf(filter.getId()), entity.getName());

		Parameters newParam = new Parameters(filter.getName().getDisplayValue());
		newParam.setReplaceable(true);
		newParam.addAttributes(VPIConstants.PARAMETER_CONDITIONS, FilterConditionFormatter.format(filter.getRootCondition()));
		newParam.addHiddenAttributes(VPIConstants.PARAMETER_REF_DIMENSIONS,
				String.join(", ", FilterConditionFormatter.referencedDimensionNames(filter.getRootCondition(), this.planning)));

		paramList.add(newParam);

		return paramList;
	}

}
