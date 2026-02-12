package com.stilog.analysevpi.model.vpsettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.utils.VPIConstants;

public class ImportExport extends FileDatas{
	boolean isImport = false;
	
	public ImportExport(String filePath, String name) {
		super(filePath, name);
	}
	
	/*
	 * PARSE XML
	 */
	@Override
	protected List<Parameters> parseXml(Entity entity) {
		
		List<Parameters> paramList = new ArrayList<>();
		Document doc = getDocument(entity.getAssociatedXml());
		
		Element firstNodes = (Element) doc.getDocumentElement().getChildNodes();

		/*
		 * Ajout des attribut unique (id, uid ...)
		 */
		String id = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_ID).item(0).getTextContent();
		String uid = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_UID).item(0).getTextContent();
		entity.addUniqueAttributes(VPIConstants.XML_TAG_ID, id);
		entity.addUniqueAttributes(VPIConstants.XML_TAG_UID, uid);
		entity.setMergeable(true);
		entity.setReplaceable(true);
		
		/*
		 * Recupération de la configuration
		 */
		Element configuration = (Element) firstNodes.getElementsByTagName(VPIConstants.XML_TAG_CONFIGURATION).item(0).getChildNodes();
		
		Node attributes = configuration.getElementsByTagName(VPIConstants.XML_TAG_EXPORTATTRIBUTES).item(0);
		if(attributes == null)
			attributes = configuration.getElementsByTagName(VPIConstants.XML_TAG_EVENTPROPERTIES).item(0);
		
		paramList.add(computeCorrespondance(attributes));
		
		paramList.addAll(computeConfiguration(configuration));
		
		return paramList;
	}

	private Parameters computeCorrespondance(Node parentNode){
		
		NodeList attributesList = parentNode.getChildNodes();
		Parameters param = new Parameters(VPIConstants.PARAMETER_CORRESPONDANCE);
		
		for(int i = 0; i < attributesList.getLength(); i++) {
			Node propertyNode = attributesList.item(i);
			
			if (propertyNode.getNodeType() != Node.ELEMENT_NODE)
	            continue;
			
			Element conf = (Element) propertyNode.getChildNodes();
			Element attribute = (Element) conf.getElementsByTagName(VPIConstants.XML_TAG_ATTRIBUTE).item(0);
			Element valueNode = (Element) conf.getElementsByTagName(VPIConstants.XML_TAG_VALUE).item(0);
			
			String name = attribute.getElementsByTagName(VPIConstants.XML_TAG_TITLE).item(0).getTextContent();
			String value = conf.getElementsByTagName(VPIConstants.XML_TAG_VALUE).item(0).getTextContent();
			
			if(valueNode.hasAttribute("isNull")) {
				Element column = (Element) conf.getElementsByTagName(VPIConstants.XML_TAG_COLUMN).item(0);
				Element sourceAttribute = (Element) conf.getElementsByTagName(VPIConstants.XML_TAG_SOURCEATTRIBUTE).item(0);
				if(!column.hasAttribute("isNull"))
					value = column.getElementsByTagName(VPIConstants.XML_TAG_NAME).item(0).getTextContent();
				else if(!sourceAttribute.hasAttribute("isNull"))
					value = sourceAttribute.getElementsByTagName(VPIConstants.XML_TAG_TITLE).item(0).getTextContent();
			}
			param.setReplaceable(true);
			param.setEditableName(false);
			param.addAttributes(name, value);
		}
		return param;
	}
	
	private List<Parameters> computeConfiguration(Node parentNode){
		List<Parameters> paramList = new ArrayList<>();
		
		Element attributesList = (Element) parentNode.getChildNodes();
		/*
		 * SOURCE
		 */
		Parameters paramSrc = new Parameters(VPIConstants.PARAMETER_SOURCE);
		Element source = (Element) attributesList.getElementsByTagName(VPIConstants.XML_TAG_SOURCECONFIG).item(0);
		NodeList paramsSource = source.getChildNodes();
		for(int i = 0; i<paramsSource.getLength();i++) {
			Node paramSource = paramsSource.item(i);
			if (paramSource.getNodeType() != Node.ELEMENT_NODE)
	            continue;
			if(this.isExcludedTag(paramSource.getNodeName()))
				continue;
			
			paramSrc.addAttributes(paramSource.getNodeName(), paramSource.getTextContent());
		}
		paramSrc.setReplaceable(true);
		paramSrc.setEditableName(false);
		paramList.add(paramSrc);
		
		/*
		 * KEY
		 */
		Parameters paramKey = new Parameters(VPIConstants.PARAMETER_KEY);
		Element keyAttr = (Element) attributesList.getElementsByTagName(VPIConstants.XML_TAG_KEY_ATTRIBUTES).item(0);
		
		NodeList keyList = keyAttr.getChildNodes();
		int keyIndex = 0;
		for(int i = 0; i<keyList.getLength(); i++) {
			if (keyList.item(i).getNodeType() != Node.ELEMENT_NODE)
	            continue;
			Element key = (Element) keyList.item(i);
			paramKey.addAttributes(VPIConstants.PARAMETER_KEY + keyIndex, key.getElementsByTagName(VPIConstants.XML_TAG_TITLE).item(0).getTextContent());
			keyIndex++;
		}
		paramKey.setReplaceable(true);
		paramKey.setEditableName(false);
		if(keyIndex != 0)
			paramList.add(paramKey);
		
		/*
		 * KEY PARENT
		 */
		Parameters paramKeyParent = new Parameters(VPIConstants.PARAMETER_KEY_PARENT);
		Element keyParentAttr = (Element) attributesList.getElementsByTagName(VPIConstants.XML_TAG_PARENT_KEY_ATTRIBUTES).item(0);
		
		if(keyParentAttr != null) {
			NodeList keyParentList = keyParentAttr.getChildNodes();
			int keyParentIndex = 0;
			for(int j = 0; j<keyParentList.getLength(); j++) {
				if (keyParentList.item(j).getNodeType() != Node.ELEMENT_NODE)
		            continue;
				Element key = (Element) keyParentList.item(j);
				paramKeyParent.addAttributes(VPIConstants.PARAMETER_KEY_PARENT + keyParentIndex, key.getElementsByTagName(VPIConstants.XML_TAG_TITLE).item(0).getTextContent());
				keyParentIndex++;
			}
			
			paramKeyParent.setReplaceable(true);
			paramKeyParent.setEditableName(false);
			if(keyParentIndex != 0)
				paramList.add(paramKeyParent);
		}
		
		/*
		 * PARAMETRE
		 */
		Parameters paramParam = new Parameters(VPIConstants.PARAMETER_PARAMETRE);
		
		//Mode d'import (Import ressources/evenements)
		Element importMode = (Element) attributesList.getElementsByTagName(VPIConstants.XML_TAG_IMPORT_MODE).item(0);
		if(importMode != null)
			paramParam.addAttributes(VPIConstants.PARAMETER_IMPORT_MODE, importMode.getTextContent());
		
		//Modele de ressource associé (Import/Export de ressource)
		Element resourceModel = (Element) attributesList.getElementsByTagName(VPIConstants.XML_TAG_RESOURCEMODEL).item(0);
		if(resourceModel != null) {
			Element entityId =  (Element) resourceModel.getElementsByTagName(VPIConstants.XML_TAG_ENTITYID).item(0);
			String resourceModelName = GeneralCorrespondance.getInstance().getCorrespondance(VPIConstants.XML_TAG_ID, entityId.getTextContent());
			
			paramParam.addAttributes(VPIConstants.PARAMETER_RESOURCEMODEL, resourceModelName);
		}
		
		//Format de date (Import/Export d'événements)
		Element dateFormat = (Element) attributesList.getElementsByTagName(VPIConstants.XML_TAG_DATE_FORMAT).item(0);
		if(dateFormat != null)
			paramParam.addAttributes(VPIConstants.PARAMETER_DATE_FORMAT, dateFormat.getTextContent());
		
		paramParam.setReplaceable(true);
		paramParam.setEditableName(false);
		paramList.add(paramParam);
		
		return paramList;
	}


	/*
	 * METHODS
	 */
	/*@Override
	public String toString() {
		return "ResourceModel [entities=" + entities + "]";
	}*/

	public boolean isImport() {
		return isImport;
	}

	public void setImport(boolean isImport) {
		this.isImport = isImport;
	}
	
}
