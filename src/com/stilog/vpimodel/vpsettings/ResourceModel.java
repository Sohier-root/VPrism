package com.stilog.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.utils.MethodUtil;
import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.TypeData;

public class ResourceModel extends FileDatas{

	private Map<String, String> attributesCorr = new HashMap<>();
	private GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
	
	public ResourceModel(String filePath, String name) {
		super(filePath, name);
	}
	
	/*
	 * PARSE XML
	 */
	@Override
	protected List<Parameters> parseXml(Entity entity) {
		//Les Dimensions sont ajoutable et remplacable
		entity.setMergeable(true);
		entity.setReplaceable(true);
		
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
		entity.setTypeData(TypeData.DIMENSION);
		
		GeneralCorrespondance.getInstance().addCorrespondance(VPIConstants.XML_TAG_ID, id, entity.getName());
		GeneralCorrespondance.getInstance().addCorrespondance(VPIConstants.XML_TAG_UID, uid, entity.getName());
		/*
		 * Ajout attribut caché
		 */
		String comments = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_COMMENTS).item(0).getTextContent();
		String activateEvntCom = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_ACTIVATE_EVT_COM).item(0).getTextContent();
		String activateCom = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_ACTIVATE_COM).item(0).getTextContent();
		String activateEvntHist = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_ACTIVATE_COM).item(0).getTextContent();
		String activateHist = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_ACTIVATE_COM).item(0).getTextContent();
		String autoFilter = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_AUTO_FILTER).item(0).getTextContent();
		String buffered = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_BUFFERED).item(0).getTextContent();
		String creaEvt = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_CREATE_EVT).item(0).getTextContent();
		
		Element treeStructNode = (Element) firstNodes.getElementsByTagName(VPIConstants.XML_TAG_TREESTRUCT).item(0);
		String treeStructId = treeStructNode.getElementsByTagName(VPIConstants.XML_TAG_ENTITYID).item(0).getTextContent();
		String treeStructName = treeStructId.equals("-1") ? "" : GeneralCorrespondance.getInstance().getCorrespondance(VPIConstants.XML_TAG_TREESTRUCT, treeStructId);
		
		String separator = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_SEPARATOR).item(0).getTextContent();
		
		Element dailyCalendarNode = (Element) firstNodes.getElementsByTagName(VPIConstants.XML_TAG_CALENDAR).item(0);
		String calendarId = dailyCalendarNode.getElementsByTagName(VPIConstants.XML_TAG_ENTITYID).item(0).getTextContent();
		String calendarName = calendarId.equals("-1") ? "" : GeneralCorrespondance.getInstance().getCorrespondance(VPIConstants.XML_TAG_CALENDAR, calendarId);
		
		Element creaRuleNode = (Element) firstNodes.getElementsByTagName(VPIConstants.XML_TAG_CREATION_RULE).item(0);
		String creaRuleId = creaRuleNode.getElementsByTagName(VPIConstants.XML_TAG_ENTITYID).item(0).getTextContent();
		String creaRuleName = creaRuleId.equals("-1") ? "" : GeneralCorrespondance.getInstance().getCorrespondance(VPIConstants.XML_TAG_CREATION_RULE, creaRuleId);
		
		Element colorNode = (Element) firstNodes.getElementsByTagName(VPIConstants.XML_TAG_COLOR).item(0);
		String colorArgb = colorNode.getElementsByTagName(VPIConstants.XML_TAG_ARGB).item(0).getTextContent();
		
		
		entity.addHiddenAttributes(VPIConstants.PARAMETER_COMMENTS, comments);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_EVT_COM, activateEvntCom);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_COM, activateCom);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_EVT_HIST, activateEvntHist);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_ACTIVATE_HIST, activateHist);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_AUTO_FILTER, autoFilter);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_BUFFERED, buffered);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_CREATE_EVT, creaEvt);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_TREESTRUCT, treeStructName);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_SEPARATOR, separator);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_CALENDAR, calendarName);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_CREATION_RULE, creaRuleName);
		entity.addHiddenAttributes(VPIConstants.PARAMETER_COLOR, colorArgb);
		
		
		/*
		 * Récupération des attributs de la dimension
		 */
		paramList.addAll(computeHeadings(firstNodes.getElementsByTagName(VPIConstants.XML_TAG_HEADINGS).item(0), entity));
		paramList.addAll(computeImportantHeadings(firstNodes, entity));
		
		return paramList;
	}
	
	/**
	 * Recupère les attributs de la dimension
	 * @param parentNode
	 * @return
	 */
	private List<Parameters> computeHeadings(Node parentNode, Entity entity){
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
			Element typeElement = ((Element)attributesList.getElementsByTagName(VPIConstants.XML_TAG_TYPE).item(0));
			String type = typeElement.getAttribute("class");
			
			Parameters newParam = new Parameters(name);
			newParam.setMergeable(true);
			newParam.setReplaceable(true);
			newParam.setParentTag(parentNode.getNodeName());
			newParam.setUid(uid);
			newParam.addAttributes(VPIConstants.PARAMETER_NAME, name);
			newParam.addAttributes(VPIConstants.PARAMETER_TYPE, type);
			
			newParam.addUniqueAttributes(VPIConstants.XML_TAG_ID, id);
			GeneralCorrespondance.getInstance().addCorrespondance(VPIConstants.XML_TAG_ID, id, name);
			newParam.addUniqueAttributes(VPIConstants.XML_TAG_UID, uid);
			GeneralCorrespondance.getInstance().addCorrespondance(VPIConstants.XML_TAG_UID, uid, name);
			
			newParam.setInitialXml(MethodUtil.nodeToString(heading));
			newParam.setAssociatedXml(MethodUtil.nodeToString(heading));
			
			//Si c'est un type resourceReference
			NodeList resourceModel = attributesList.getElementsByTagName(VPIConstants.XML_TAG_RESOURCEMODEL);
			if(resourceModel != null && resourceModel.getLength() > 0) {
				Element resourceModelParam = (Element) resourceModel.item(0).getChildNodes();
				String idResourceModel = resourceModelParam.getElementsByTagName(VPIConstants.XML_TAG_ENTITYID).item(0).getTextContent();
				String resourceModelName = gCorr.getCorrespondance(VPIConstants.PARAMETER_RESOURCEMODEL, idResourceModel);
				entity.addResourceModelAttributes(idResourceModel); //Ajout de l'id de la dimension pour correspondances
				newParam.addAttributes(VPIConstants.PARAMETER_RESOURCEMODEL, resourceModelName!=null?resourceModelName:idResourceModel);
				
			}
			
			//Liste à choix unique
			NodeList uniqueValues = attributesList.getElementsByTagName(VPIConstants.XML_TAG_UNIQUE_VALUE_LIST);
			if(uniqueValues != null && uniqueValues.getLength() > 0) {
				NodeList uniqueValueList = uniqueValues.item(0).getChildNodes();
				String uniqueListStr = "";
				for(int j = 0; j<uniqueValueList.getLength(); j++) {
					if (uniqueValueList.item(j).getNodeType() != Node.ELEMENT_NODE)
			            continue;
					Element itemList = (Element) uniqueValueList.item(j);
					String itemName = itemList.getElementsByTagName(VPIConstants.XML_TAG_VALUE).item(0).getTextContent();
					uniqueListStr += ", " + itemName;
				}
				newParam.addAttributes(VPIConstants.PARAMETER_VALUE_LIST, uniqueListStr.replaceFirst(", ", ""));
			}
			
			//Liste à choix multiple
			NodeList multipleValues = attributesList.getElementsByTagName(VPIConstants.XML_TAG_MULTI_VALUE_LIST);
			if(multipleValues != null && multipleValues.getLength() > 0) {
				NodeList multipleValuesList = multipleValues.item(0).getChildNodes();
				String uniqueListStr = "";
				for(int j = 0; j<multipleValuesList.getLength(); j++) {
					if (multipleValuesList.item(j).getNodeType() != Node.ELEMENT_NODE)
			            continue;
					Element itemList = (Element) multipleValuesList.item(j);
					String itemName = itemList.getElementsByTagName(VPIConstants.XML_TAG_VALUE).item(0).getTextContent();
					uniqueListStr += ", " + itemName;
				}
				newParam.addAttributes(VPIConstants.PARAMETER_VALUE_LIST, uniqueListStr.replaceFirst(", ", ""));
			}
			
			/*
			 * Attribut caché
			 */
			String comments = attributesList.getElementsByTagName(VPIConstants.XML_TAG_COMMENTS).item(0).getTextContent();
			String indexed = attributesList.getElementsByTagName(VPIConstants.XML_TAG_INDEXED).item(0).getTextContent();
			String defaultVal = attributesList.getElementsByTagName(VPIConstants.XML_TAG_DEFAULT_VAL).item(0).getTextContent();
			String forbiddenDefVal = attributesList.getElementsByTagName(VPIConstants.XML_TAG_FORBIDDEN_DEFAULT_VAL).item(0).getTextContent();
			Node lenghtNode = typeElement.getElementsByTagName(VPIConstants.XML_TAG_LENGHT).item(0);
			String lenght = lenghtNode!=null ? lenghtNode.getTextContent() : "";
			Node patternNode = attributesList.getElementsByTagName(VPIConstants.XML_TAG_PATTERN).item(0);
			String pattern = patternNode!=null ? patternNode.getTextContent() : "";
			
			newParam.addHiddenAttributes(VPIConstants.PARAMETER_COMMENTS, comments);
			newParam.addHiddenAttributes(VPIConstants.PARAMETER_INDEXED, indexed);
			newParam.addHiddenAttributes(VPIConstants.PARAMETER_DEFAULT_VAL, defaultVal);
			newParam.addHiddenAttributes(VPIConstants.PARAMETER_FORBIDDEN_DEFAULT_VAL, forbiddenDefVal);
			newParam.addHiddenAttributes(VPIConstants.PARAMETER_LENGHT, lenght);
			newParam.addHiddenAttributes(VPIConstants.PARAMETER_PATTERN, pattern);
			
			headings.add(newParam);
			
			attributesCorr.put(id, name);
			
		}
		return headings;
	}
	
	private List<Parameters> computeImportantHeadings(Element parentNode, Entity entity){
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
		Parameters keyParam = new Parameters(VPIConstants.PARAMETER_KEY, MethodUtil.nodeToString(keyNode));
		keyParam.setReplaceable(true);
		keyParam.setEditableName(false);
		keyParam.setDocumentable(false);
		String keysStr = "";
		for(int i = 0; i<keyList.getLength(); i++) {
			Node key = keyList.item(i);
			String idAttribute =  key.getTextContent();
			String nameAttribute = attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute;
			keyParam.addAttributes(VPIConstants.PARAMETER_KEY + String.valueOf(i), 
					nameAttribute);
			keysStr += ", " + nameAttribute;
		}
		entity.addHiddenAttributes(VPIConstants.PARAMETER_KEY, keysStr.replaceFirst(", ", ""));
		
		//KEY HEADINGS
		Parameters keyHeadingsParam = new Parameters(VPIConstants.PARAMETER_KEYHEADINGS, MethodUtil.nodeToString(keyHeadingsNode));
		keyHeadingsParam.setReplaceable(true);
		keyHeadingsParam.setEditableName(false);
		keyHeadingsParam.setDocumentable(false);
		String keysHeadingStr = "";
		for(int i = 0; i<keyHeadingsList.getLength(); i++) {
			Node key = keyHeadingsList.item(i);
			String idAttribute =  key.getTextContent();
			String nameAttribute = attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute;
			keyHeadingsParam.addAttributes(VPIConstants.PARAMETER_KEYHEADINGS + String.valueOf(i), 
					nameAttribute);
			keysHeadingStr += ", " + nameAttribute;
		}
		entity.addHiddenAttributes(VPIConstants.PARAMETER_KEYHEADINGS, keysHeadingStr.replaceFirst(", ", ""));
		
		//LABEL HEADINGS
		Parameters labelsHeadingsParam = new Parameters(VPIConstants.PARAMETER_LABELSHEADINGS, MethodUtil.nodeToString(labelsHeadingsNode));
		labelsHeadingsParam.setReplaceable(true);
		labelsHeadingsParam.setEditableName(false);
		labelsHeadingsParam.setDocumentable(false);
		String labelsHeadingStr = "";
		for(int i = 0; i<labelsHeadingsList.getLength(); i++) {
			Node key = labelsHeadingsList.item(i);
			String idAttribute = key.getTextContent();
			String nameAttribute = attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute;
			labelsHeadingsParam.addAttributes(VPIConstants.PARAMETER_LABELSHEADINGS + String.valueOf(i), 
					nameAttribute);
			labelsHeadingStr += ", " + nameAttribute;
		}
		entity.addHiddenAttributes(VPIConstants.PARAMETER_LABELSHEADINGS, labelsHeadingStr.replaceFirst(", ", ""));
		
		//MANDATORY
		Parameters mandatoryParam = new Parameters(VPIConstants.PARAMETER_MANDATORY, MethodUtil.nodeToString(mandatoryNode));
		mandatoryParam.setReplaceable(true);
		mandatoryParam.setEditableName(false);
		mandatoryParam.setDocumentable(false);
		String mandatoryStr = "";
		for(int i = 0; i<mandatoryList.getLength(); i++) {
			Node key = mandatoryList.item(i);
			String idAttribute =  key.getTextContent();
			String nameAttribute = attributesCorr.containsKey(idAttribute)?attributesCorr.get(idAttribute):idAttribute;
			mandatoryParam.addAttributes(VPIConstants.PARAMETER_MANDATORY + String.valueOf(i), 
					nameAttribute);
			mandatoryStr += ", " + nameAttribute;
		}
		entity.addHiddenAttributes(VPIConstants.PARAMETER_MANDATORY, mandatoryStr.replaceFirst(", ", ""));
		
		importantHeadings.add(keyParam);
		importantHeadings.add(keyHeadingsParam);
		importantHeadings.add(labelsHeadingsParam);
		importantHeadings.add(mandatoryParam);
		
		return importantHeadings;
	}

	
}
