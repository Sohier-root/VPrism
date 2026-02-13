package com.stilog.documentation.model.dto;

public class StyleInfo {
    private boolean hasHeaderRow;
    private String headerBackgroundColor;
    private String borderColor;
    private Integer borderWidth;
    
    // Constructeurs
    public StyleInfo() {}
    
    // Getters et Setters
    public boolean isHasHeaderRow() { return hasHeaderRow; }
    public void setHasHeaderRow(boolean hasHeaderRow) { 
        this.hasHeaderRow = hasHeaderRow; 
    }
    
    public String getHeaderBackgroundColor() { return headerBackgroundColor; }
    public void setHeaderBackgroundColor(String headerBackgroundColor) { 
        this.headerBackgroundColor = headerBackgroundColor; 
    }
    
    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String borderColor) { 
        this.borderColor = borderColor; 
    }
    
    public Integer getBorderWidth() { return borderWidth; }
    public void setBorderWidth(Integer borderWidth) { 
        this.borderWidth = borderWidth; 
    }
}
