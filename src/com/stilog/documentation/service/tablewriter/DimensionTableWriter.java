package com.stilog.documentation.service.tablewriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.documentation.model.dto.TableData;
import com.stilog.documentation.util.ExcelDocumentUtil;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

public class DimensionTableWriter {

	public static List<TableData> fillTables(Map<String, TableData> dimensionTables, Entity entity , List<Parameters> parameters){
		List<TableData> resultTableList = new ArrayList<>();
		
         	TableData dimensionTemplate = fillDimensionTable(entity, dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_DIMENSION).clone());
         	
         	resultTableList.add(dimensionTemplate);
         	
         	// TODO Remplir le clone avec les données
         	for(Parameters param : parameters) {
         		TableData template = null;
         		switch(param.getAttributeValue(VPIConstants.PARAMETER_TYPE)) {
         		case "StringType":
         			template = fillStringTable(param, dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_STRING).clone());
         			break;
         		case "UniqueChoiceType":
         			template = fillUniqueChoiceTable(param, dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_CHOIX_UNIQUE).clone());
         		case "MultipleChoiceType":
         			template = fillMultipleChoiceTable(param, dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_CHOIX_MULTI).clone());
         		case "BooleanType":
         			template = dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_BOOLEAN).clone();
         			template.setData(ExcelDocumentUtil.RUB_NOM, param.getName());
         			break;
         		case "com.visualplanning.data.heading.FileType":
         			template = dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_ATTACHEMENT).clone();
         			template.setData(ExcelDocumentUtil.RUB_NOM, param.getName());
         			break;
         		}
         		if(template != null)
         			resultTableList.add(template);
         	}
		 
		return resultTableList;
	}
	
	private static TableData fillDimensionTable(Entity entity, TableData template) {
		template.setData(ExcelDocumentUtil.RUB_DIMENSION, entity.getName());
		template.setData(ExcelDocumentUtil.RUB_NOM, entity.getName());
		template.setData(ExcelDocumentUtil.RUB_COMMENTAIRES, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_COMMENTS));
		
		/*
		 * Commentaire
		 */
		template.setData(ExcelDocumentUtil.RUB_ACTIVE_EVT, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_ACTIVATE_EVT_COM));
		template.setData(ExcelDocumentUtil.RUB_ACTIVE, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_ACTIVATE_COM));
		
		/*
		 * Historique
		 */
		template.setData(ExcelDocumentUtil.RUB_ACTIVE_EVT, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_ACTIVATE_EVT_HIST), 1);
		template.setData(ExcelDocumentUtil.RUB_ACTIVE, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_ACTIVATE_HIST), 1);
		
		/*
		 * Spécifiques
		 */
		template.setData(ExcelDocumentUtil.RUB_FILTRE_RAPIDE, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_AUTO_FILTER));
		template.setData(ExcelDocumentUtil.RUB_CHARGER_TOUT, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_BUFFERED));
		template.setData(ExcelDocumentUtil.RUB_CREA_EVT, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_CREATE_EVT));
		template.setData(ExcelDocumentUtil.RUB_HIERARCHIE, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_TREESTRUCT));
		
		/*
		 * Valeurs par défaut
		 */
		template.setData(ExcelDocumentUtil.RUB_COLOR, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_COLOR));
		template.setData(ExcelDocumentUtil.RUB_CALENDAR, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_CALENDAR));
		template.setData(ExcelDocumentUtil.RUB_EVT_CREA_RULE, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_CREATION_RULE));
		
		/*
		 * Rubriques
		 */
		String keys = entity.getParameter(VPIConstants.PARAMETER_KEY).getAttributes()
			    .stream()
			    .map(attr -> attr.getValue())
			    .collect(Collectors.joining(", "));
		
		String keysHeading = entity.getParameter(VPIConstants.PARAMETER_KEYHEADINGS).getAttributes()
			    .stream()
			    .map(attr -> attr.getValue())
			    .collect(Collectors.joining(", "));
		
		String mandatory = entity.getParameter(VPIConstants.PARAMETER_MANDATORY).getAttributes()
			    .stream()
			    .map(attr -> attr.getValue())
			    .collect(Collectors.joining(", "));
		
		template.setData(ExcelDocumentUtil.RUB_KEYS, keys);
		template.setData(ExcelDocumentUtil.RUB_KEYS_HEADINGS, keysHeading);
		template.setData(ExcelDocumentUtil.RUB_MANDATORY, mandatory);	
		
		/*
		 * Libellés
		 */
		String labelsHeading = entity.getParameter(VPIConstants.PARAMETER_LABELSHEADINGS).getAttributes()
			    .stream()
			    .map(attr -> attr.getValue())
			    .collect(Collectors.joining(", "));
		template.setData(ExcelDocumentUtil.RUB_LABELS_HEADINGS, labelsHeading);
		template.setData(ExcelDocumentUtil.RUB_SEPARATOR, entity.getHiddenAttributeValue(VPIConstants.PARAMETER_SEPARATOR));
		
		return template;
	}
	
	private static TableData fillStringTable(Parameters param, TableData template) {
		template.setData(ExcelDocumentUtil.RUB_RUBRIQUE_TEXTE, param.getName());
		template.setData(ExcelDocumentUtil.RUB_NOM, param.getName());
		template.setData(ExcelDocumentUtil.RUB_DESCRIPTION, param.getAttributeValue(VPIConstants.PARAMETER_COMMENTS));
		template.setData(ExcelDocumentUtil.RUB_INDEXATION, param.getAttributeValue(VPIConstants.PARAMETER_INDEXED));
		template.setData(ExcelDocumentUtil.RUB_VAL_DEFAUT, param.getAttributeValue(VPIConstants.PARAMETER_DEFAULT_VAL));
		template.setData(ExcelDocumentUtil.RUB_INT_VAL_DEFAUT, param.getAttributeValue(VPIConstants.PARAMETER_FORBIDDEN_DEFAULT_VAL));
		template.setData(ExcelDocumentUtil.RUB_FORMAT, param.getAttributeValue(VPIConstants.PARAMETER_PATTERN));
		
		return template;
	}
	
	private static TableData fillUniqueChoiceTable(Parameters param, TableData template) {
		template.setData(ExcelDocumentUtil.RUB_RUBRIQUE_CHOIX_UNIQUE, param.getName());
		template.setData(ExcelDocumentUtil.RUB_NOM, param.getName());
		template.setData(ExcelDocumentUtil.RUB_DESCRIPTION, param.getAttributeValue(VPIConstants.PARAMETER_COMMENTS));
		template.setData(ExcelDocumentUtil.RUB_INDEXATION, param.getAttributeValue(VPIConstants.PARAMETER_INDEXED));
		template.setData(ExcelDocumentUtil.RUB_VAL_DEFAUT, param.getAttributeValue(VPIConstants.PARAMETER_DEFAULT_VAL));
		template.setData(ExcelDocumentUtil.RUB_INT_VAL_DEFAUT, param.getAttributeValue(VPIConstants.PARAMETER_FORBIDDEN_DEFAULT_VAL));
		template.setData(ExcelDocumentUtil.RUB_FILTRE_AUTO, param.getAttributeValue(VPIConstants.PARAMETER_AUTO_FILTER));
		template.setData(ExcelDocumentUtil.RUB_LISTE_VALEURS, param.getAttributeValue(VPIConstants.PARAMETER_VALUE_LIST));
		
		return template;
	}
	
	private static TableData fillMultipleChoiceTable(Parameters param, TableData template) {
		template.setData(ExcelDocumentUtil.RUB_RUBRIQUE_CHOIX_UNIQUE, param.getName());
		template.setData(ExcelDocumentUtil.RUB_NOM, param.getName());
		template.setData(ExcelDocumentUtil.RUB_DESCRIPTION, param.getAttributeValue(VPIConstants.PARAMETER_COMMENTS));
		template.setData(ExcelDocumentUtil.RUB_INDEXATION, param.getAttributeValue(VPIConstants.PARAMETER_INDEXED));
		template.setData(ExcelDocumentUtil.RUB_VAL_DEFAUT, param.getAttributeValue(VPIConstants.PARAMETER_DEFAULT_VAL));
		template.setData(ExcelDocumentUtil.RUB_INT_VAL_DEFAUT, param.getAttributeValue(VPIConstants.PARAMETER_FORBIDDEN_DEFAULT_VAL));
		template.setData(ExcelDocumentUtil.RUB_FILTRE_AUTO, param.getAttributeValue(VPIConstants.PARAMETER_AUTO_FILTER));
		template.setData(ExcelDocumentUtil.RUB_LISTE_VALEURS, param.getAttributeValue(VPIConstants.PARAMETER_VALUE_LIST));
		
		return template;
	}
}
