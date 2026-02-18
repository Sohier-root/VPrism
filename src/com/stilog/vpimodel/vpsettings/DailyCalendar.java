package com.stilog.vpimodel.vpsettings;

import java.util.List;

import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

public class DailyCalendar extends FileDatas {

	public DailyCalendar(String filePath, String name) {
		super(filePath, name);
		this.setHidden(true);
	}

	@Override
	protected List<Parameters> parseXml(Entity entity) {
		GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
		gCorr.addCorrespondance(VPIConstants.XML_TAG_CALENDAR, String.valueOf(entity.getId()), entity.getName());
		return null;
	}

}
