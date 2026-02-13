package com.stilog.documentation.model.dto;

import java.util.List;
import java.util.Map;

public class DocumentProcessingRequest {
    private String wordTemplatePath;
    private String excelTemplatePath;
    private List<String> tableNames; // Tables spécifiques à copier
    private Map<String, Object> replacementParameters;
    private String outputPath;
}
