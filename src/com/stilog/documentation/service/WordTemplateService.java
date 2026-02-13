package com.stilog.documentation.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import com.stilog.documentation.model.dto.TableData;

public interface WordTemplateService {
    XWPFDocument loadTemplate(String templatePath);
    void insertTable(XWPFDocument document, TableData tableData, 
                    int position);
    void saveDocument(XWPFDocument document, String outputPath);
}
