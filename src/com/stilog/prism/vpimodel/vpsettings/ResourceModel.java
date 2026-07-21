package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;

import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.TypeData;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.dimension.Dimension;
import com.visualplanning.vpi.model.dimension.Heading;
import com.visualplanning.vpi.model.property.PropertyList;

public class ResourceModel extends FileDatas{

	public ResourceModel(String filePath, String name) {
		super(filePath, name);
	}

	/*
	 * PARSE (via VPIReader)
	 */
	@Override
	protected List<Parameters> buildParameters(Entity entity) {
		entity.setTypeData(TypeData.DIMENSION);

		Dimension dim = findDimension(entity.getId());
		if (dim == null) {
			System.out.println("Dimension introuvable pour l'entité id=" + entity.getId());
			return new ArrayList<>();
		}

		List<Parameters> paramList = new ArrayList<>();

		/*
		 * Attributs cachés
		 */
		entity.addHiddenAttributes(VPIConstants.PARAMETER_COMMENTS, dim.getDescription().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_EVT_COM, dim.getActivateEventForum().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_COM, dim.getActivateForum().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_EVT_HIST, dim.getActivatedEventHistoryTracker().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_HIST, dim.getActivatedHistoryTracker().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_AUTO_FILTER, dim.getAutoFilter().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_BUFFERED, dim.getBuffered().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_CREATE_EVT, dim.getCreateEvent().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_TREESTRUCT, dim.getEventHierarchy().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_SEPARATOR, dim.getSeparator().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_CALENDAR, dim.getDailyCalendar().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_CREATION_RULE, dim.getEventCreationRule().getDisplayValue());
		entity.addHiddenAttributes(VPIConstants.PARAMETER_COLOR, dim.getColorArgb().getDisplayValue());

		/*
		 * Rubriques (headings)
		 */
		paramList.addAll(computeHeadingParameters(dim.getHeadings()));
		paramList.addAll(computeImportantHeadings(entity, dim));

		return paramList;
	}

	private Dimension findDimension(int id) {
		for (Dimension d : this.planning.getDimensions()) {
			if (d.getId() == id)
				return d;
		}
		return null;
	}

	private List<Parameters> computeImportantHeadings(Entity entity, Dimension dim) {
		List<Parameters> importantHeadings = new ArrayList<>();

		importantHeadings.add(headingRefParameters(entity, VPIConstants.PARAMETER_KEY, dim.getKeyHeadingIds(), dim, true));
		importantHeadings.add(headingRefParameters(entity, VPIConstants.PARAMETER_KEYHEADINGS, dim.getKeyHeadingIds(), dim, false));
		importantHeadings.add(headingRefParameters(entity, VPIConstants.PARAMETER_LABELSHEADINGS, dim.getLabelsHeadingIds(), dim, false));
		importantHeadings.add(headingRefParameters(entity, VPIConstants.PARAMETER_MANDATORY, dim.getMandatoryHeadingIds(), dim, false));

		return importantHeadings;
	}

	private Parameters headingRefParameters(Entity entity, String parameterName, PropertyList<Integer> ids, Dimension dim, boolean alsoHidden) {
		Parameters param = new Parameters(parameterName);

		List<Integer> idList = ids != null ? ids.getValue() : List.of();
		List<String> names = new ArrayList<>();
		for (int i = 0; i < idList.size(); i++) {
			String headingName = headingNameById(dim, idList.get(i));
			param.addAttributes(parameterName + i, headingName);
			names.add(headingName);
		}
		String joined = String.join(", ", names);
		entity.addHiddenAttributes(parameterName, joined);
		if (alsoHidden)
			entity.addHiddenAttributes(VPIConstants.PARAMETER_KEY, joined);

		return param;
	}

	private String headingNameById(Dimension dim, int id) {
		for (Heading h : dim.getHeadings()) {
			if (h.getId() == id)
				return h.getName();
		}
		return String.valueOf(id);
	}
}
