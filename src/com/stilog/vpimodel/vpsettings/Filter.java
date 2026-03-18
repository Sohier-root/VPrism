package com.stilog.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.utils.MethodUtil;
import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

public class Filter extends FileDatas{

	/**
	 * Clé de correspondance utilisée dans GeneralCorrespondance.
	 * Différente selon le type de filtre (ressource ou événement),
	 * définie par la sous-classe via setFilterCorrespondanceKey().
	 */
	private String filterCorrespondanceKey = VPIConstants.XML_TAG_FILTER_RESOURCE;

	public Filter(String filePath, String name) {
		super(filePath, name);
	}

	public void setFilterCorrespondanceKey(String key) {
		this.filterCorrespondanceKey = key;
	}

	/*
	 * PARSE XML
	 */
	@Override
	protected List<Parameters> parseXml(Entity entity) {
		
		List<Parameters> paramList = new ArrayList<>();
		try {
			Document doc = getDocument(entity.getAssociatedXml());
			Element firstNodes = (Element) doc.getDocumentElement().getChildNodes();
			
			/*
			 * Ajout des attribut unique (id, uid ...)
			 */
			String id = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_ID).item(0).getTextContent();
			String uid = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_UID).item(0).getTextContent();
			entity.addUniqueAttributes(VPIConstants.XML_TAG_ID, id);
			entity.addUniqueAttributes(VPIConstants.XML_TAG_UID, uid);
			entity.setReplaceable(true);
			entity.setMergeable(true);
			
			/*
			 * Enregistrement dans GeneralCorrespondance pour résolution INFILTER
			 * Clé : filterCorrespondanceKey (ressource ou événement), valeur : id → nom
			 */
			GeneralCorrespondance.getInstance().addCorrespondance(
				filterCorrespondanceKey, id, entity.getName());
			
			/*
			 * Recupération des conditions du filtre
			 */
			String name = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_NAME).item(0).getTextContent();
			Node conditonsNode = firstNodes.getElementsByTagName(VPIConstants.XML_TAG_FILTERCONDITION).item(0);
			Parameters newParam = new Parameters(name);
			newParam.setReplaceable(true);
			newParam.addAttributes(VPIConstants.PARAMETER_CONDITIONS, MethodUtil.nodeToString(conditonsNode));
			
			paramList.add(newParam);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return paramList;
	}

}
