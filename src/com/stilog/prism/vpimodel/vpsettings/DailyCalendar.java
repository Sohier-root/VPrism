package com.stilog.prism.vpimodel.vpsettings;

import java.util.List;

import com.stilog.prism.comparevpi.model.GeneralCorrespondance;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.utils.VPIConstants;

public class DailyCalendar extends FileDatas {

	public DailyCalendar(String filePath, String name) {
		super(filePath, name);
		this.setHidden(true);
	}

	@Override
	protected List<Parameters> parseXml(Entity entity) {
		GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
		String name = entity.getName();

		if (this.planning != null) {
			name = this.planning.getCalendarSet().dailyCalendarById(entity.getId())
					.map(c -> c.getName().getDisplayValue())
					.orElse(entity.getName());
		}

		gCorr.addCorrespondance(VPIConstants.XML_TAG_CALENDAR, String.valueOf(entity.getId()), name);
		return null;
	}

}
