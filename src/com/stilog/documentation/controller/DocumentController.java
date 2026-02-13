package com.stilog.documentation.controller;

import java.util.Map;

import com.stilog.documentation.model.dto.DocumentProcessingRequest;
import com.stilog.documentation.model.dto.DocumentProcessingResponse;
import com.stilog.documentation.service.DocumentService;

public class DocumentController {
	
    private final DocumentService documentService = null;
    
    public DocumentProcessingResponse processDocuments(
        DocumentProcessingRequest request) {
        // Orchestration du traitement TODO
    	return null;
    }
    
    public void validateTemplates(String wordTemplatePath, 
                                  String excelTemplatePath) {
        // Validation des templates TODO
    }
}
