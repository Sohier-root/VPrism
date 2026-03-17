package com.stilog.documentation.model.dto;

import java.util.ArrayList;
import java.util.List;

import com.stilog.documentation.model.dto.style.CellStyleInfo;
import com.stilog.documentation.model.dto.style.StyleInfo;

public class TableData {
    private String tableName;
    private int rowCount;
    private int columnCount;
    private List<List<String>> data;
    
    // Informations de style 
    private StyleInfo styleInfo;
    private List<List<CellStyleInfo>> cellStyles;
    
    // Constructeurs
    public TableData() {}
    
    public TableData(String tableName, List<List<String>> data) {
        this.tableName = tableName;
        this.data = data;
        this.rowCount = data.size();
        this.columnCount = data.isEmpty() ? 0 : data.get(0).size();
        this.cellStyles = new ArrayList<>();
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
    public void setData(String name, String value) {
    	this.setData(name, value, 0);
    }
    public void setData(String name, String value, int index) {
    	for(List<String> datas : data) {
    		if(datas.contains(name)) {
    			if(index <= 0) {
    			datas.set(datas.size()-1, value);
    			return;
    			}
    			else {
    				index --;
    			}
    		}
    	}
    }
    
    public StyleInfo getStyleInfo() { return styleInfo; }
    public void setStyleInfo(StyleInfo styleInfo) { this.styleInfo = styleInfo; }
    
    public List<List<CellStyleInfo>> getCellStyles() { return cellStyles; }
    public void setCellStyles(List<List<CellStyleInfo>> cellStyles) { 
        this.cellStyles = cellStyles; 
    }
    
    public CellStyleInfo getCellStyle(int row, int col) {
        if (row < cellStyles.size() && col < cellStyles.get(row).size()) {
            return cellStyles.get(row).get(col);
        }
        return null;
    }
    
    public TableData clone() {
        // Deep copy de data
        List<List<String>> newDatas = new ArrayList<>();
        for (List<String> row : this.data) {
            newDatas.add(new ArrayList<>(row));
        }

        // Deep copy de cellStyles
        List<List<CellStyleInfo>> newCellStyles = new ArrayList<>();
        for (List<CellStyleInfo> row : this.cellStyles) {
            newCellStyles.add(new ArrayList<>(row));
        }

        TableData clone = new TableData(this.getTableName(), newDatas);
        clone.setCellStyles(newCellStyles);
        clone.setStyleInfo(this.styleInfo);
        clone.setRowCount(this.rowCount);
        clone.setColumnCount(this.columnCount);

        return clone;
    }
}
