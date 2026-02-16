package com.stilog.documentation.service.impl;

import com.stilog.documentation.service.*;
import com.stilog.documentation.service.tablewriter.DimensionTableWriter;
import com.stilog.documentation.model.dto.*;
import com.stilog.documentation.exception.*;
import com.stilog.documentation.util.ExcelDocumentUtil;
import com.stilog.documentation.util.TableConverter;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;
import java.lang.reflect.Parameter;
import java.util.*;

public class DocumentServiceImpl implements DocumentService {
    
	private static final String EXCEL_TEMPLATE_PATH = "/com/stilog/documentation/templates/template.xlsx";
	private static final String WORD_TEMPLATE_PATH = "/com/stilog/documentation/templates/template.docx";
	
    private final WordTemplateService wordTemplateService;
    private final ExcelTemplateService excelTemplateService;
    
    public DocumentServiceImpl(WordTemplateService wordTemplateService, 
                              ExcelTemplateService excelTemplateService) {
        this.wordTemplateService = wordTemplateService;
        this.excelTemplateService = excelTemplateService;
    }
    
    @Override
    public DocumentProcessingResponse processTemplates(
            String outputPath,
            Map<Entity, List<Parameters>> paramToInsert) {
        
        DocumentProcessingResponse response = new DocumentProcessingResponse();
        List<String> errors = new ArrayList<>();
        
        try {
            // 1. Charger le template Word
            XWPFDocument wordDocument = wordTemplateService.loadTemplate(WORD_TEMPLATE_PATH);
            
            // 2. Extraire les tableaux depuis Excel
            Map<String, TableData> dimensionTables = excelTemplateService.extractTables(EXCEL_TEMPLATE_PATH, ExcelDocumentUtil.DIMENSION_SHEET);
            
            if (dimensionTables.isEmpty()) {
                throw new TableExtractionException("Aucun tableau trouvé dans le fichier Excel");
            }
            
            // 3. Remplir les tableaux
            for(Entity entity : paramToInsert.keySet()) {
            	List<Parameters> parameters = paramToInsert.get(entity);
            	
            	switch(entity.getTypeData()) {
            	case DIMENSION :
            		List<TableData> resultTableList = DimensionTableWriter.fillTables(dimensionTables, entity, parameters);
            		List<String> titleList = new ArrayList<>();
            		titleList.add("PARAMÉTRAGE NÉCÉSSAIRE");
            		titleList.add("Dimension");
            		titleList.add(entity.getName());
            		for(TableData table : resultTableList) {
            			wordTemplateService.insertTable(wordDocument, table, titleList);
            		}
            		break;
            	}
            }
            
            wordTemplateService.saveDocument(wordDocument, outputPath);
            
            // 5. Construire la réponse
            response.setSuccess(errors.isEmpty());
            response.setOutputFilePath(outputPath);
            response.setTablesProcessed(dimensionTables.size() - errors.size());
            response.setErrors(errors);
            response.setMessage(errors.isEmpty() ? 
                "Traitement réussi" : 
                "Traitement terminé avec des erreurs");
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Erreur lors du traitement: " + e.getMessage());
            errors.add(e.getMessage());
            response.setErrors(errors);
            e.printStackTrace();
        }
        
        return response;
    }
    
    private String generateOutputPath(String templatePath) {
        int dotIndex = templatePath.lastIndexOf('.');
        String basePath = templatePath.substring(0, dotIndex);
        return basePath + "_output.docx";
    }
}
