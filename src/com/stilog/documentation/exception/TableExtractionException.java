package com.stilog.documentation.exception;

public class TableExtractionException extends RuntimeException{

    private String errorCode;
    
    public TableExtractionException(String message) {
        super(message);
    }
    
    public TableExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
