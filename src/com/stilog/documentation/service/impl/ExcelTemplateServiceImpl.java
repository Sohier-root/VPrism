package com.stilog.documentation.service.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import com.stilog.documentation.exception.*;
import com.stilog.documentation.model.dto.TableData;
import com.stilog.documentation.model.dto.style.CellStyleInfo;
import com.stilog.documentation.model.dto.style.StyleInfo;
import com.stilog.documentation.service.ExcelTemplateService;
import com.stilog.documentation.util.DocumentUtil;

public class ExcelTemplateServiceImpl implements ExcelTemplateService {
    
    @Override
    public Map<String, TableData> extractTables(String excelPath, String sheetName) {
        List<TableData> tables = new ArrayList<>();
        
        try (InputStream is = DocumentUtil.getResourceAsStream(excelPath);
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            
                XSSFSheet sheet = workbook.getSheet(sheetName);
                
                // Vérifier s'il y a des tableaux définis
                List<XSSFTable> sheetTables = sheet.getTables();
                
                if (!sheetTables.isEmpty()) {
                    // Extraire les tableaux définis
                    for (XSSFTable table : sheetTables) {
                        TableData tableData = extractDefinedTable(table, sheet);
                        tables.add(tableData);
                    }
                } else {
                    // Extraire la feuille entière comme un tableau
                    TableData tableData = extractSheetAsTable(sheet);
                    if (tableData.getRowCount() > 0) {
                        tables.add(tableData);
                    }
                }
            
        } catch (FileNotFoundException e) {
            throw new TemplateNotFoundException(
                "Template Excel non trouvé: " + excelPath, e);
        } catch (IOException e) {
            throw new TableExtractionException(
                "Erreur lors de la lecture du fichier Excel", e);
        }
        
        Map<String, TableData> result = new HashMap<>();
        for(TableData table : tables) {
        	result.put(table.getTableName(), table);
        }
        return result;
    }
    
    /**
     * Extrait un tableau défini (XSSFTable)
     */
    private TableData extractDefinedTable(XSSFTable table, XSSFSheet sheet) {
        TableData tableData = new TableData();
        tableData.setTableName(table.getDisplayName() != null ? 
            table.getDisplayName() : table.getName());
        
        List<List<String>> data = new ArrayList<>();
        List<List<CellStyleInfo>> cellStyles = new ArrayList<>();
        
        // Obtenir la zone du tableau
        int startRow = table.getStartRowIndex();
        int endRow = table.getEndRowIndex();
        int startCol = table.getStartColIndex();
        int endCol = table.getEndColIndex();
        
        // Extraire les données
        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            List<String> rowData = new ArrayList<>();
            List<CellStyleInfo> rowStyles = new ArrayList<>();
            
            if (row != null) {
                for (int colIndex = startCol; colIndex <= endCol; colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    rowData.add(getCellValueAsString(cell));
                    
                    // Extraire le style
                    if (cell != null) {
                        rowStyles.add(extractCellStyle(cell));
                    } else {
                        rowStyles.add(new CellStyleInfo());
                    }
                }
            }
            data.add(rowData);
            cellStyles.add(rowStyles);
        }
        
        tableData.setData(data);
        tableData.setCellStyles(cellStyles);
        tableData.setRowCount(data.size());
        tableData.setColumnCount(data.isEmpty() ? 0 : data.get(0).size());
        
        // Extraire les informations de style
        StyleInfo styleInfo = extractStyleInfo(sheet, startRow);
        tableData.setStyleInfo(styleInfo);
        
        return tableData;
    }
    
    /**
     * Extrait une feuille entière comme tableau
     */
    private TableData extractSheetAsTable(XSSFSheet sheet) {
        TableData tableData = new TableData();
        tableData.setTableName(sheet.getSheetName());
        
        List<List<String>> data = new ArrayList<>();
        List<List<CellStyleInfo>> cellStyles = new ArrayList<>();
        
        int lastRowNum = sheet.getLastRowNum();
        int maxColumns = 0;
        
        // Déterminer le nombre maximum de colonnes
        for (int i = 0; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                maxColumns = Math.max(maxColumns, row.getLastCellNum());
            }
        }
        
        // Extraire les données
        for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            List<String> rowData = new ArrayList<>();
            List<CellStyleInfo> rowStyles = new ArrayList<>();
            
            if (row != null) {
                for (int colIndex = 0; colIndex < maxColumns; colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    rowData.add(getCellValueAsString(cell));
                    
                    // Extraire le style
                    if (cell != null) {
                        rowStyles.add(extractCellStyle(cell));
                    } else {
                        rowStyles.add(new CellStyleInfo());
                    }
                }
                
                // Vérifier si la ligne n'est pas vide
                boolean isEmpty = rowData.stream().allMatch(s -> s == null || s.trim().isEmpty());
                if (!isEmpty) {
                    data.add(rowData);
                    cellStyles.add(rowStyles);
                }
            }
        }
        
        tableData.setData(data);
        tableData.setCellStyles(cellStyles);
        tableData.setRowCount(data.size());
        tableData.setColumnCount(maxColumns);
        
        // Extraire les informations de style
        StyleInfo styleInfo = extractStyleInfo(sheet, 0);
        tableData.setStyleInfo(styleInfo);
        
        return tableData;
    }
    
    /**
     * Convertit une cellule en String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Éviter l'affichage des décimales pour les nombres entiers
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    }
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    /**
     * Extrait les informations de style de la première ligne
     */
    private StyleInfo extractStyleInfo(XSSFSheet sheet, int headerRowIndex) {
        StyleInfo styleInfo = new StyleInfo();
        styleInfo.setHasHeaderRow(true);
        
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow != null && headerRow.getFirstCellNum() >= 0) {
            Cell firstCell = headerRow.getCell(headerRow.getFirstCellNum());
            if (firstCell != null) {
                CellStyle cellStyle = firstCell.getCellStyle();
                
                // Extraire la couleur de fond
                if (cellStyle.getFillForegroundColorColor() instanceof XSSFColor) {
                    XSSFColor color = (XSSFColor) cellStyle.getFillForegroundColorColor();
                    if (color != null && color.getARGBHex() != null) {
                        styleInfo.setHeaderBackgroundColor(color.getARGBHex().substring(2));
                    }
                }
            }
        }
        
        return styleInfo;
    }
    
    /**
     * Extrait le style d'une cellule Excel
     */
    private CellStyleInfo extractCellStyle(Cell cell) {
        CellStyleInfo styleInfo = new CellStyleInfo();
        CellStyle cellStyle = cell.getCellStyle();
        
        if (cellStyle == null) {
            return styleInfo;
        }
        
        // Couleur de fond
        if (cellStyle.getFillForegroundColorColor() instanceof XSSFColor) {
            XSSFColor bgColor = (XSSFColor) cellStyle.getFillForegroundColorColor();
            if (bgColor != null && bgColor.getARGBHex() != null) {
                styleInfo.setBackgroundColor(bgColor.getARGBHex().substring(2));
            }
        }
        
        // Police
        if (cell.getSheet().getWorkbook() instanceof XSSFWorkbook) {
            XSSFWorkbook workbook = (XSSFWorkbook) cell.getSheet().getWorkbook();
            XSSFFont font = workbook.getFontAt(cellStyle.getFontIndexAsInt());
            
            if (font != null) {
                // Couleur du texte
                XSSFColor fontColor = font.getXSSFColor();
                if (fontColor != null && fontColor.getARGBHex() != null) {
                    styleInfo.setTextColor(fontColor.getARGBHex().substring(2));
                }
                
                // Gras et italique
                styleInfo.setBold(font.getBold());
                styleInfo.setItalic(font.getItalic());
                
                // Famille et taille de police
                styleInfo.setFontFamily(font.getFontName());
                styleInfo.setFontSize((int) font.getFontHeightInPoints());
            }
        }
        
        // Alignement horizontal
        switch (cellStyle.getAlignment()) {
            case LEFT:
                styleInfo.setAlignment("left");
                break;
            case CENTER:
                styleInfo.setAlignment("center");
                break;
            case RIGHT:
                styleInfo.setAlignment("right");
                break;
            default:
                styleInfo.setAlignment("left");
        }
        
        // Alignement vertical
        switch (cellStyle.getVerticalAlignment()) {
            case TOP:
                styleInfo.setVerticalAlignment("top");
                break;
            case CENTER:
                styleInfo.setVerticalAlignment("center");
                break;
            case BOTTOM:
                styleInfo.setVerticalAlignment("bottom");
                break;
            default:
                styleInfo.setVerticalAlignment("top");
        }
        
        // Bordures
        styleInfo.setHasTopBorder(cellStyle.getBorderTop() != BorderStyle.NONE);
        styleInfo.setHasBottomBorder(cellStyle.getBorderBottom() != BorderStyle.NONE);
        styleInfo.setHasLeftBorder(cellStyle.getBorderLeft() != BorderStyle.NONE);
        styleInfo.setHasRightBorder(cellStyle.getBorderRight() != BorderStyle.NONE);
        
        // Couleur de bordure
        /*if (cellStyle.getTopBorderXSSFColor() != null) {
            XSSFColor borderColor = cellStyle.getTopBorderXSSFColor();
            if (borderColor.getARGBHex() != null) {
                styleInfo.setBorderColor(borderColor.getARGBHex().substring(2));
            }
        }*/
        
        return styleInfo;
    }
}
