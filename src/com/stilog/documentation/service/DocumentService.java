package com.stilog.documentation.service;

import java.util.Map;

import com.stilog.documentation.model.dto.DocumentProcessingResponse;

public interface DocumentService {
    DocumentProcessingResponse processTemplates(
            String wordTemplatePath, 
            String excelTemplatePath,
            Map<String, Object> parameters
        );
}
