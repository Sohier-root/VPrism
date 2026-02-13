package com.stilog.documentation.util;

import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import com.stilog.documentation.model.dto.TableData;

public class TableConverter {
    /**
     * Convertit une table Excel (Apache POI) en TableData
     */
    public static TableData convertExcelTable(XSSFTable excelTable) {
        // Logique de conversion TODO
    	return null;
    }
    
    /**
     * Convertit TableData en table Word (Apache POI)
     */
    public static XWPFTable convertToWordTable(
        XWPFDocument document, TableData tableData) {
        // Logique de conversion TODO
    	return null;
    }
}
