package com.stilog.documentation.model;

import java.io.File;

import com.stilog.vpimodel.objects.TypeFile;
import com.stilog.vpimodel.objects.VPIDatas;

public class DocumentationModel {
	
	private VPIDatas datas = new VPIDatas();
	
	public void processFile(File file) {
		datas.computeDatas(file, TypeFile.DOCUMENTATION);
	}
	
	public VPIDatas getData() {
		return datas;
	}
}
