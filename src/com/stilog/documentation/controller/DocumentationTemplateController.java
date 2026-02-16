package com.stilog.documentation.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stilog.documentation.model.DocumentationModel;
import com.stilog.documentation.model.dto.DocumentProcessingRequest;
import com.stilog.documentation.model.dto.DocumentProcessingResponse;
import com.stilog.documentation.service.DocumentService;
import com.stilog.documentation.service.ExcelTemplateService;
import com.stilog.documentation.service.WordTemplateService;
import com.stilog.documentation.service.impl.DocumentServiceImpl;
import com.stilog.documentation.service.impl.ExcelTemplateServiceImpl;
import com.stilog.documentation.service.impl.WordTemplateServiceImpl;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

public class DocumentationTemplateController {
	
    private final DocumentService documentService = null;
    
    public DocumentationTemplateController() {
    }
    
    public DocumentProcessingResponse processDocuments(
        DocumentProcessingRequest request,
        Map<Entity, List<Parameters>> paramToInsert) {
       ExcelTemplateService excelService = new ExcelTemplateServiceImpl();
       WordTemplateService wordService = new WordTemplateServiceImpl();
       DocumentService docService = new DocumentServiceImpl(wordService, excelService);
       docService.processTemplates(request.getOutputPath(), paramToInsert);
    	return null;
    }
}
