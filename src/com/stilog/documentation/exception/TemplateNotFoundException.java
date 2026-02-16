package com.stilog.documentation.exception;

public class TemplateNotFoundException extends RuntimeException {

    private String errorCode;
    
    public TemplateNotFoundException(String message) {
        super(message);
    }
    
    public TemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
