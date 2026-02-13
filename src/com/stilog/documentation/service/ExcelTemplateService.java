package com.stilog.documentation.service;

import java.util.List;

import com.stilog.documentation.model.dto.TableData;

public interface ExcelTemplateService {
    List<TableData> extractTables(String excelPath);
    TableData extractTableByName(String excelPath, String tableName);
}
