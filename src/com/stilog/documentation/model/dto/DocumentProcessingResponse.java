package com.stilog.documentation.model.dto;

import java.util.List;

public class DocumentProcessingResponse {
    private boolean success;
    private String outputFilePath;
    private int tablesProcessed;
    private String message;
    private List<String> errors;
    
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public String getOutputFilePath() {
		return outputFilePath;
	}
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}
	public int getTablesProcessed() {
		return tablesProcessed;
	}
	public void setTablesProcessed(int tablesProcessed) {
		this.tablesProcessed = tablesProcessed;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<String> getErrors() {
		return errors;
	}
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
    
}
