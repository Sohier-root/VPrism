package com.stilog.documentation.util;


import java.math.BigInteger;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVerticalJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;

import com.stilog.documentation.model.dto.TableData;
import com.stilog.documentation.model.dto.style.CellStyleInfo;

public class TableConverter {
    
    /**
     * Convertit TableData en table Word (XWPFTable)
     */
    public static XWPFTable convertToWordTable(XWPFDocument document, TableData tableData) {
        List<List<String>> data = tableData.getData();
        
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("TableData ne contient aucune donnée");
        }
        
        int rows = data.size();
        int cols = data.get(0).size();
        
        // Créer le tableau
        XWPFTable table = document.createTable(rows, cols);
        
        // Remplir le tableau avec les données
        for (int i = 0; i < rows; i++) {
            XWPFTableRow row = table.getRow(i);
            List<String> rowData = data.get(i);
            
            for (int j = 0; j < cols && j < rowData.size(); j++) {
            	XWPFTableCell cell = row.getCell(j);
                String cellValue = rowData.get(j);
                if(i == 0)
                	cellValue = cellValue.replaceAll("Colonne\\d+", "");
                
                setCellText(cell, cellValue);
                
                // Appliquer le style de la cellule
                CellStyleInfo cellStyle = tableData.getCellStyle(i, j);
                if (cellStyle != null) {
                    applyCellStyle(cell, cellStyle);
                }
            }
        }
        
        return table;
    }
    
    private static void setCellText(XWPFTableCell cell, String value) {
        // Valeur à afficher (jamais null)
        String displayValue = (value != null) ? value : "";
        
        // Récupérer le premier paragraphe de la cellule
        XWPFParagraph paragraph;
        if (cell.getParagraphs().isEmpty()) {
            paragraph = cell.addParagraph();
        } else {
            paragraph = cell.getParagraphs().get(0);
        }
        
        // Supprimer les runs existants
        int runCount = paragraph.getRuns().size();
        for (int i = runCount - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        XWPFRun run = paragraph.createRun();
        run.setText(displayValue, 0); // ← Le "0" force l'insertion à la position 0
    }
    
    /**
     * Applique le style à une cellule Word
     */
    private static void applyCellStyle(XWPFTableCell cell, CellStyleInfo styleInfo) {
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
        }
        
        // Couleur de fond
        if (styleInfo.getBackgroundColor() != null && !styleInfo.getBackgroundColor().isEmpty()) {
            CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
            shd.setFill(styleInfo.getBackgroundColor());
            shd.setVal(STShd.CLEAR);
        }
        
        // Alignement vertical
        if (styleInfo.getVerticalAlignment() != null) {
            CTVerticalJc vAlign = tcPr.isSetVAlign() ? tcPr.getVAlign() : tcPr.addNewVAlign();
            switch (styleInfo.getVerticalAlignment()) {
                case "top":
                    vAlign.setVal(STVerticalJc.TOP);
                    break;
                case "center":
                    vAlign.setVal(STVerticalJc.CENTER);
                    break;
                case "bottom":
                    vAlign.setVal(STVerticalJc.BOTTOM);
                    break;
            }
        }
        
        // Bordures
        CTTcBorders borders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
        String borderColor = styleInfo.getBorderColor() != null ? 
            styleInfo.getBorderColor() : "000000";
        
        if (styleInfo.isHasTopBorder()) {
            configureCellBorder(borders.addNewTop(), borderColor);
        }
        if (styleInfo.isHasBottomBorder()) {
            configureCellBorder(borders.addNewBottom(), borderColor);
        }
        if (styleInfo.isHasLeftBorder()) {
            configureCellBorder(borders.addNewLeft(), borderColor);
        }
        if (styleInfo.isHasRightBorder()) {
            configureCellBorder(borders.addNewRight(), borderColor);
        }
        
        // Style du texte
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            // Alignement horizontal
            if (styleInfo.getAlignment() != null) {
                switch (styleInfo.getAlignment()) {
                    case "left":
                        paragraph.setAlignment(ParagraphAlignment.LEFT);
                        break;
                    case "center":
                        paragraph.setAlignment(ParagraphAlignment.CENTER);
                        break;
                    case "right":
                        paragraph.setAlignment(ParagraphAlignment.RIGHT);
                        break;
                }
            }
            
            // Police, couleur, gras, italique
            for (XWPFRun run : paragraph.getRuns()) {
                if (styleInfo.getTextColor() != null && !styleInfo.getTextColor().isEmpty()) {
                    run.setColor(styleInfo.getTextColor());
                }
                
                if (styleInfo.isBold()) {
                    run.setBold(true);
                }
                
                if (styleInfo.isItalic()) {
                    run.setItalic(true);
                }
                
                if (styleInfo.getFontFamily() != null) {
                    run.setFontFamily(styleInfo.getFontFamily());
                }
                
                if (styleInfo.getFontSize() != null) {
                    run.setFontSize(styleInfo.getFontSize());
                }
            }
        }
    }
    
    /**
     * Configure une bordure de cellule
     */
    private static void configureCellBorder(CTBorder border, String color) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setSpace(BigInteger.ZERO);
        border.setColor(color);
    }
}
