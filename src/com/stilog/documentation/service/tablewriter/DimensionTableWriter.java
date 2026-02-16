package com.stilog.documentation.service.tablewriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.documentation.model.dto.TableData;
import com.stilog.documentation.util.ExcelDocumentUtil;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

public class DimensionTableWriter {

	public static List<TableData> fillTables(Map<String, TableData> dimensionTables, Entity entity , List<Parameters> parameters){
		List<TableData> resultTableList = new ArrayList<>();
		
         	TableData dimensionTemplate = dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_DIMENSION).clone();
         	dimensionTemplate.setData(ExcelDocumentUtil.RUB_DIMENSION, entity.getName());
         	
         	resultTableList.add(dimensionTemplate);
         	
         	// TODO Remplir le clone avec les données
         	for(Parameters param : parameters) {
         		TableData template = null;
         		switch(param.getAttributeValue(VPIConstants.PARAMETER_TYPE)) {
         		case "StringType":
         			template = dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_STRING).clone();
         			template.setData(ExcelDocumentUtil.RUB_NOM, param.getName());
         			break;
         		case "BooleanType":
         			template = dimensionTables.get(ExcelDocumentUtil.TABLE_NAME_BOOLEAN).clone();
         			template.setData(ExcelDocumentUtil.RUB_NOM, param.getName());
         			break;
         		}
         		if(template != null)
         			resultTableList.add(template);
         	}
		 
		return resultTableList;
	}
}
