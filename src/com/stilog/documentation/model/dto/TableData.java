package com.stilog.documentation.model.dto;

import java.util.List;

public class TableData {
    private String tableName;
    private int rowCount;
    private int columnCount;
    private List<List<String>> data;
    
    // Informations de style (optionnel)
    private StyleInfo styleInfo;
    
    // Constructeurs
    public TableData() {}
    
    public TableData(String tableName, List<List<String>> data) {
        this.tableName = tableName;
        this.data = data;
        this.rowCount = data.size();
        this.columnCount = data.isEmpty() ? 0 : data.get(0).size();
    }
    
    // Getters et Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    
    public int getColumnCount() { return columnCount; }
    public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
    
    public List<List<String>> getData() { return data; }
    public void setData(List<List<String>> data) { this.data = data; }
    
    public StyleInfo getStyleInfo() { return styleInfo; }
    public void setStyleInfo(StyleInfo styleInfo) { this.styleInfo = styleInfo; }
}
