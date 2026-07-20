package com.stilog.prism.vpimodel.vpsettings;

import java.util.List;

import com.stilog.prism.comparevpi.model.GeneralCorrespondance;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.utils.VPIConstants;

public class CreationRule extends FileDatas {

	public CreationRule(String filePath, String name) {
		super(filePath, name);
		this.setHidden(true);
	}

	@Override
	protected List<Parameters> buildParameters(Entity entity) {
		GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
		gCorr.addCorrespondance(VPIConstants.XML_TAG_CREATION_RULE, String.valueOf(entity.getId()), entity.getName());
		return null;
	}

}
