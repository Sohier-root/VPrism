package com.stilog.documentation.model.dto;

import java.util.List;

public class DocumentProcessingResponse {
    private boolean success;
    private String outputFilePath;
    private int tablesProcessed;
    private String message;
    private List<String> errors;
}
