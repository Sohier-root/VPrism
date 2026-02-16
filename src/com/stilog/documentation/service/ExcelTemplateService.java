package com.stilog.documentation.service;

import java.util.List;
import java.util.Map;

import com.stilog.documentation.model.dto.TableData;

public interface ExcelTemplateService {
    Map<String, TableData> extractTables(String excelPath, String sheetName);
}
