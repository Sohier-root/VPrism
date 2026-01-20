package com.stilog.analysevpi.controller;

import java.io.File;

import com.stilog.analysevpi.model.Model;
import com.stilog.analysevpi.model.comparator.VPIComparator;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.model.objects.PositionFile;
import com.stilog.analysevpi.model.objects.VPIDatas;
import com.stilog.analysevpi.model.vpsettings.FileDatas;

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
	
	public VPIDatas mergeParameter(FileDatas file, Entity entity, Parameters parameter, Parameters parameterToReplace) {
		VPIDatas rightDatas = model.getData(PositionFile.RIGHT);
		rightDatas.mergeParameter(file, entity, parameter, parameterToReplace);
		return rightDatas;
	}
	
	public VPIDatas mergeEntity(FileDatas file, Entity entity, Entity entityToReplace) {
		VPIDatas rightDatas = model.getData(PositionFile.RIGHT);
		rightDatas.mergeEntity(file, entity, entityToReplace);
		return rightDatas;
	}
}
