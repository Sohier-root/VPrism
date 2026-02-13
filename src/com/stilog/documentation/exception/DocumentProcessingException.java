package com.stilog.documentation.exception;

public class DocumentProcessingException extends RuntimeException {
    private String errorCode;
    
    public DocumentProcessingException(String message) {
        super(message);
    }
    
    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
