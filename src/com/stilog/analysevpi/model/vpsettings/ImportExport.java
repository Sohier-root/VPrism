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

public class ImportExport extends FileDatas{
	boolean isImport = false;
	
	public ImportExport(String filePath, String name) {
		super(filePath, name);
	}
	
	/*
	 * PARSING METHODS
	 */
	protected List<Parameters> parseXml(String xml) {
		
		List<Parameters> paramList = new ArrayList<>();
		Document doc = getDocument(xml);
		
		Element firstNodes = (Element) doc.getDocumentElement().getChildNodes();
		//String name = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_NAME).item(0).getTextContent();
		Element configuration = (Element) firstNodes.getElementsByTagName("configuration").item(0).getChildNodes();
		
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
		Parameters param = new Parameters(VPIConstants.PARAMETER_SOURCE);
		Element source = (Element) attributesList.getElementsByTagName(VPIConstants.XML_TAG_SOURCECONFIG).item(0);
		NodeList paramsSource = source.getChildNodes();
		for(int i = 0; i<paramsSource.getLength();i++) {
			Node paramSource = paramsSource.item(i);
			if (paramSource.getNodeType() != Node.ELEMENT_NODE)
	            continue;
			if(this.isExcludedTag(paramSource.getNodeName()))
				continue;
			
			param.addAttributes(paramSource.getNodeName(), paramSource.getTextContent());
		}
		paramList.add(param);
		
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
