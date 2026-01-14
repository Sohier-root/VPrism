package com.stilog.analysevpi.controller;

import java.io.File;

import com.stilog.analysevpi.model.Model;
import com.stilog.analysevpi.model.comparator.VPIComparator;
import com.stilog.analysevpi.model.objects.PositionFile;
import com.stilog.analysevpi.model.objects.VPIDatas;

public class Controller {

	private Model model;
	
	public Controller(Model model) {
		this.model = model;
	}
	
	public VPIDatas handleFile(File file, PositionFile position) {
		model.processFile(file, position);
		
		return getVPIData(position);
	}
	
	public VPIDatas getVPIData(PositionFile position) {
		return model.getData(position);
	}
	
	public VPIDatas performComparison() {
		VPIComparator.compare(model);
		return model.getData(PositionFile.LEFT);
	}
}
