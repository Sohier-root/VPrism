package com.stilog.analysevpi.model.vpsettings;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.utils.MethodUtil;
import com.stilog.analysevpi.utils.VPIConstants;

public class Filter extends FileDatas{

	public Filter(String filePath, String name) {
		super(filePath, name);
	}

	@Override
	protected List<Parameters> parseXml(String xml) {
		
		List<Parameters> paramList = new ArrayList<>();
		try {
			Document doc = getDocument(xml);
			Element firstNodes = (Element) doc.getDocumentElement().getChildNodes();
			
			String name = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_NAME).item(0).getTextContent();
			Node conditonsNode = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_FILTERCONDITION).item(0);
			
			Parameters newParam = new Parameters(name);
			newParam.addAttributes(VPIConstants.PARAMETER_CONDITIONS, MethodUtil.nodeToString(conditonsNode));
			
			paramList.add(newParam);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return paramList;
	}

}
