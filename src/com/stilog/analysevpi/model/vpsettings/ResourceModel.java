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
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.utils.VPIConstants;

public class ResourceModel extends FileDatas{

	private Map<String, String> attributesCorr = new HashMap<>();
	private GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
	
	public ResourceModel(String filePath, String name) {
		super(filePath, name);
	}
	
	/*
	 * PARSING METHODS
	 */
	protected List<Parameters> parseXml(String xml) {
		
		List<Parameters> paramList = new ArrayList<>();
		Document doc = getDocument(xml);
		
		Element firstNodes = (Element) doc.getDocumentElement().getChildNodes();
		paramList.addAll(computeHeadings(firstNodes.getElementsByTagName(VPIConstants.XML_TAG_HEADINGS).item(0)));
		paramList.addAll(computeImportantHeadings(firstNodes));
		
		return paramList;
	}
	
	private List<Parameters> computeHeadings(Node parentNode){
		List<Parameters> headings = new ArrayList<>();
		
		NodeList headingsList = parentNode.getChildNodes();
		
		for(int i = 0; i < headingsList.getLength(); i++) {
			Node heading = headingsList.item(i);
			if (heading.getNodeType() != Node.ELEMENT_NODE)
	            continue;

			Element attributesList = (Element) heading.getChildNodes();
			
			String id = attributesList.getElementsByTagName(VPIConstants.XML_TAG_ID).item(0).getTextContent();
			String uid = attributesList.getElementsByTagName(VPIConstants.XML_TAG_UID).item(0).getTextContent();
			String name = attributesList.getElementsByTagName(VPIConstants.XML_TAG_NAME).item(0).getTextContent();
			String type = ((Element)attributesList.getElementsByTagName(VPIConstants.XML_TAG_TYPE).item(0)).getAttribute("class");
			
			Parameters newParam = new Parameters(name);
			newParam.setUid(uid);
			newParam.addAttributes(VPIConstants.PARAMETER_NAME, name);
			newParam.addAttributes(VPIConstants.PARAMETER_TYPE, type);
			
			//Si c'est un type resourceReference
			NodeList resourceModel = attributesList.getElementsByTagName(VPIConstants.XML_TAG_RESOURCEMODEL);
			if(resourceModel != null && resourceModel.getLength() > 0) {
				Element resourceModelParam = (Element) resourceModel.item(0).getChildNodes();
				String idResourceModel = resourceModelParam.getElementsByTagName(VPIConstants.XML_TAG_ENTITYID).item(0).getTextContent();
				String resourceModelName = gCorr.getResourceModelName(idResourceModel);
				newParam.addAttributes(VPIConstants.PARAMETER_RESOURCEMODEL, resourceModelName!=null?resourceModelName:idResourceModel);
				
			}
			
			headings.add(newParam);
			
			//Ajout des correspondance de BDD (rub)
			String column = attributesList.getElementsByTagName(VPIConstants.XML_TAG_COLUMNNAME).item(0).getTextContent();
			GeneralCorrespondance.getInstance().addRub(column, name);
			
			attributesCorr.put(id, name);
			
		}
		return headings;
	}
	
	private List<Parameters> computeImportantHeadings(Element parentNode){
		List<Parameters> importantHeadings = new ArrayList<>();
		
		Node keyNode = parentNode.getElementsByTagName(VPIConstants.XML_TAG_KEY).item(0);
		Node keyHeadingsNode = parentNode.getElementsByTagName(VPIConstants.XML_TAG_KEYHEADINGS).item(0);
		Node labelsHeadingsNode = parentNode.getElementsByTagName(VPIConstants.XML_TAG_LABELSHEADINGS).item(0);
		Node mandatoryNode = parentNode.getElementsByTagName(VPIConstants.XML_TAG_MANDATORY).item(0);
		
		NodeList keyList = ((Element) keyNode.getChildNodes()).getElementsByTagName(VPIConstants.XML_TAG_ENTITYID);
		NodeList keyHeadingsList = ((Element) keyHeadingsNode.getChildNodes()).getElementsByTagName(VPIConstants.XML_TAG_ENTITYID);
		NodeList labelsHeadingsList = ((Element) labelsHeadingsNode.getChildNodes()).getElementsByTagName(VPIConstants.XML_TAG_ENTITYID);
		NodeList mandatoryList = ((Element) mandatoryNode.getChildNodes()).getElementsByTagName(VPIConstants.XML_TAG_ENTITYID);
		
		//KEY
		Parameters keyParam = new Parameters(VPIConstants.PARAMETER_KEY);
		for(int i = 0; i<keyList.getLength(); i++) {
			Node key = keyList.item(i);
			String idAttribute =  key.getTextContent();
			keyParam.addAttributes(VPIConstants.PARAMETER_KEY + String.valueOf(i), 
					attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute);
		}
		
		//KEY HEADINGS
		Parameters keyHeadingsParam = new Parameters(VPIConstants.PARAMETER_KEYHEADINGS);
		for(int i = 0; i<keyHeadingsList.getLength(); i++) {
			Node key = keyHeadingsList.item(i);
			String idAttribute =  key.getTextContent();
			keyHeadingsParam.addAttributes(VPIConstants.PARAMETER_KEYHEADINGS + String.valueOf(i), 
					attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute);
		}
		
		//LABEL HEADINGS
		Parameters labelsHeadingsParam = new Parameters(VPIConstants.PARAMETER_LABELSHEADINGS);
		for(int i = 0; i<labelsHeadingsList.getLength(); i++) {
			Node key = labelsHeadingsList.item(i);
			String idAttribute = key.getTextContent();
			labelsHeadingsParam.addAttributes(VPIConstants.PARAMETER_LABELSHEADINGS + String.valueOf(i), 
					attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute);
		}
		
		//LABEL HEADINGS
		Parameters mandatoryParam = new Parameters(VPIConstants.PARAMETER_MANDATORY);
		for(int i = 0; i<mandatoryList.getLength(); i++) {
			Node key = mandatoryList.item(i);
			String idAttribute =  key.getTextContent();
			mandatoryParam.addAttributes(VPIConstants.PARAMETER_MANDATORY + String.valueOf(i), 
					attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute);
		}
		
		importantHeadings.add(keyParam);
		importantHeadings.add(keyHeadingsParam);
		importantHeadings.add(labelsHeadingsParam);
		importantHeadings.add(mandatoryParam);
		
		return importantHeadings;
	}



	/*
	 * METHODS
	 */
	/*@Override
	public String toString() {
		return "ResourceModel [entities=" + entities + "]";
	}*/
	
}
