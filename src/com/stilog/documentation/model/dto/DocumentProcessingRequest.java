package com.stilog.documentation.model.dto;

import java.util.List;
import java.util.Map;

public class DocumentProcessingRequest {
    private String wordTemplatePath;
    private String excelTemplatePath;
    private List<String> tableNames; // Tables spécifiques à copier
    private String outputPath;
    
    
    
	public DocumentProcessingRequest(String outputPath) {
		super();
		this.outputPath = outputPath;
	}
	
	/*
	 * GETTER & SETTER
	 */
	public String getWordTemplatePath() {
		return wordTemplatePath;
	}
	public void setWordTemplatePath(String wordTemplatePath) {
		this.wordTemplatePath = wordTemplatePath;
	}
	public String getExcelTemplatePath() {
		return excelTemplatePath;
	}
	public void setExcelTemplatePath(String excelTemplatePath) {
		this.excelTemplatePath = excelTemplatePath;
	}
	public List<String> getTableNames() {
		return tableNames;
	}
	public void setTableNames(List<String> tableNames) {
		this.tableNames = tableNames;
	}
	public String getOutputPath() {
		return outputPath;
	}
	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}
    
    
}
