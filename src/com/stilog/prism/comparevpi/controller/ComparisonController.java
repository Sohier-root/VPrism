package com.stilog.prism.comparevpi.controller;

import java.io.File;

import com.stilog.prism.comparevpi.model.ComparisonModel;
import com.stilog.prism.comparevpi.model.comparator.VPIComparator;
import com.stilog.prism.vpimodel.objects.TypeFile;
import com.stilog.prism.vpimodel.objects.VPIDatas;

public class ComparisonController {

	private ComparisonModel model;

	public ComparisonController(ComparisonModel model) {
		this.model = model;
	}

	public VPIDatas handleFile(File file, TypeFile position) {
		model.processFile(file, position);
		return getVPIData(position);
	}

	public VPIDatas getVPIData(TypeFile position) {
		return model.getData(position);
	}
	
	public VPIDatas performComparison() {
		VPIComparator.compare(model);
		return model.getData(TypeFile.COMPARISON_LEFT);
	}
	
	public void performReverse() {
		model.reverse();
	}
}
