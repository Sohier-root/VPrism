package com.stilog.documentation.model.dto.style;

public class StyleInfo {
	
    private boolean hasHeaderRow;
    private String headerBackgroundColor;
    private String headerTextColor;
    private String borderColor;
    private Integer borderWidth;
    
    // Nouveaux attributs pour un style complet
    private String bodyBackgroundColor;
    private String bodyTextColor;
    private boolean isBold;
    private boolean isItalic;
    private String fontFamily;
    private Integer fontSize;
    private String alignment; // "left", "center", "right"
    private String verticalAlignment; // "top", "center", "bottom"
    
    // Pour les lignes alternées
    private boolean hasAlternateRows;
    private String alternateRowColor;
    
    public StyleInfo() {}
    
    // Getters et Setters
    public boolean isHasHeaderRow() { return hasHeaderRow; }
    public void setHasHeaderRow(boolean hasHeaderRow) { this.hasHeaderRow = hasHeaderRow; }
    
    public String getHeaderBackgroundColor() { return headerBackgroundColor; }
    public void setHeaderBackgroundColor(String color) { this.headerBackgroundColor = color; }
    
    public String getHeaderTextColor() { return headerTextColor; }
    public void setHeaderTextColor(String color) { this.headerTextColor = color; }
    
    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String borderColor) { this.borderColor = borderColor; }
    
    public Integer getBorderWidth() { return borderWidth; }
    public void setBorderWidth(Integer borderWidth) { this.borderWidth = borderWidth; }
    
    public String getBodyBackgroundColor() { return bodyBackgroundColor; }
    public void setBodyBackgroundColor(String color) { this.bodyBackgroundColor = color; }
    
    public String getBodyTextColor() { return bodyTextColor; }
    public void setBodyTextColor(String color) { this.bodyTextColor = color; }
    
    public boolean isBold() { return isBold; }
    public void setBold(boolean bold) { isBold = bold; }
    
    public boolean isItalic() { return isItalic; }
    public void setItalic(boolean italic) { isItalic = italic; }
    
    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }
    
    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }
    
    public String getAlignment() { return alignment; }
    public void setAlignment(String alignment) { this.alignment = alignment; }
    
    public String getVerticalAlignment() { return verticalAlignment; }
    public void setVerticalAlignment(String verticalAlignment) { 
        this.verticalAlignment = verticalAlignment; 
    }
    
    public boolean isHasAlternateRows() { return hasAlternateRows; }
    public void setHasAlternateRows(boolean hasAlternateRows) { 
        this.hasAlternateRows = hasAlternateRows; 
    }
    
    public String getAlternateRowColor() { return alternateRowColor; }
    public void setAlternateRowColor(String color) { this.alternateRowColor = color; }
}
