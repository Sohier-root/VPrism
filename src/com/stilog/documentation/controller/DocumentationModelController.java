package com.stilog.documentation.controller;

import java.io.File;

import com.stilog.documentation.model.DocumentationModel;
import com.stilog.vpimodel.objects.TypeFile;
import com.stilog.vpimodel.objects.VPIDatas;

public class DocumentationModelController {

	private DocumentationModel model;
	
    public DocumentationModelController(DocumentationModel model) {
    	this.model = model;
    }
    
    public void handleFile(File file) {
    	model.processFile(file);
    }
    
	
	public VPIDatas getVPIData() {
		return model.getData();
	}
}
