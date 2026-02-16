package com.stilog.documentation.service;

import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.stilog.documentation.model.dto.TableData;

public interface WordTemplateService {
    XWPFDocument loadTemplate(String templatePath);
    void insertTable(XWPFDocument document, TableData tableData, List<String> titleHierarchy);
    void saveDocument(XWPFDocument document, String outputPath);
}
