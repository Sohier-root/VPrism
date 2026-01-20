package com.stilog.analysevpi.model.vpsettings;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.utils.MethodUtil;
import com.stilog.analysevpi.utils.VPIConstants;

public class Hierarchies extends FileDatas {

	public Hierarchies(String filePath, String name) {
		super(filePath, name);
	}

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
		
		/*
		 * Récupération de la structure
		 */
		paramList.addAll(computeStruct(firstNodes.getElementsByTagName(VPIConstants.XML_TAG_EVENT_STRUCT).item(0)));
		
		return paramList;
	}
	
	private List<Parameters> computeStruct(Node parentNode) {
		List<Parameters> paramList = new ArrayList<>();
		
		Element structChild = (Element) parentNode.getChildNodes();
		NodeList resourceStructNodes = structChild.getElementsByTagName(VPIConstants.XML_TAG_MODEL_STRUCT).item(0).getChildNodes();
		
		for(int i = 0 ; i<resourceStructNodes.getLength(); i++) {
			
			if (resourceStructNodes.item(i).getNodeType() != Node.ELEMENT_NODE)
	            continue;
			
			Element resource = (Element) resourceStructNodes.item(i);
			
			String resourceId = ((Element)resource.getElementsByTagName(VPIConstants.XML_TAG_RESOURCEMODEL).item(0)).getElementsByTagName(VPIConstants.XML_TAG_ENTITYID).item(0).getTextContent();
			String resourceName = GeneralCorrespondance.getInstance().getCorrespondance(VPIConstants.PARAMETER_RESOURCEMODEL, resourceId);
			String mandatory = resource.getElementsByTagName(VPIConstants.XML_TAG_MANDATORY).item(0).getTextContent();
			
			Parameters param = new Parameters(resourceName);
			param.addAttributes(VPIConstants.PARAMETER_CONDITIONS, computeConditions(resource.getElementsByTagName(VPIConstants.XML_TAG_FILTER).item(0)));
			param.addAttributes(VPIConstants.PARAMETER_MANDATORY, mandatory);
			paramList.add(param);
		}
		
		return paramList;
	}
	
	private String computeConditions(Node parentNode) {
		try {
			Element filterNodes = (Element) parentNode.getChildNodes();
			if(!filterNodes.hasChildNodes())
				return "";
			Element filterConditionsNodes = (Element) ((Element) filterNodes.getElementsByTagName(VPIConstants.XML_TAG_FILTERCONDITION).item(0)).getChildNodes();
			Node conditionsNode = filterConditionsNodes.getElementsByTagName(VPIConstants.XML_TAG_CONDITIONS).item(0);
			
			return MethodUtil.nodeToString(conditionsNode);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}
}
