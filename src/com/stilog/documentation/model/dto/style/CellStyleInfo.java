package com.stilog.documentation.model.dto.style;

public class CellStyleInfo {
    private String backgroundColor;
    private String textColor;
    private boolean isBold;
    private boolean isItalic;
    private String fontFamily;
    private Integer fontSize;
    private String alignment; // "left", "center", "right"
    private String verticalAlignment;
    private boolean hasTopBorder;
    private boolean hasBottomBorder;
    private boolean hasLeftBorder;
    private boolean hasRightBorder;
    private String borderColor;
    
    public CellStyleInfo() {}
    
    // Getters et Setters
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String color) { this.backgroundColor = color; }
    
    public String getTextColor() { return textColor; }
    public void setTextColor(String color) { this.textColor = color; }
    
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
    
    public boolean isHasTopBorder() { return hasTopBorder; }
    public void setHasTopBorder(boolean hasTopBorder) { this.hasTopBorder = hasTopBorder; }
    
    public boolean isHasBottomBorder() { return hasBottomBorder; }
    public void setHasBottomBorder(boolean hasBottomBorder) { 
        this.hasBottomBorder = hasBottomBorder; 
    }
    
    public boolean isHasLeftBorder() { return hasLeftBorder; }
    public void setHasLeftBorder(boolean hasLeftBorder) { this.hasLeftBorder = hasLeftBorder; }
    
    public boolean isHasRightBorder() { return hasRightBorder; }
    public void setHasRightBorder(boolean hasRightBorder) { 
        this.hasRightBorder = hasRightBorder; 
    }
    
    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String borderColor) { this.borderColor = borderColor; }
}
