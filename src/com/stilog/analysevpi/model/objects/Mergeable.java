package com.stilog.analysevpi.model.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Mergeable {

	private Map<String, String> uniqueAttributes = new HashMap<>();
	
	private List<String> resourceModelAttributes = new ArrayList<>();
	
	private String parentTag;
	private String associatedXml;
	private String initialXml;
	
	private boolean added = false;
	
	/*
	 * Ajoutable ou remplacable
	 */
	private boolean mergeable = false; //L'objet est-il ajoutable au VPI
	private boolean replaceable = false; //L'objet est-il remplacable dans le VPI
	private boolean editableName = true; //Le nom de l'objet peu-il etre modifié
	
	public Map<String, String> getUniqueAttributes() {
		return uniqueAttributes;
	}
	
	public String getUniqueAttribute(String name) {
		return uniqueAttributes.get(name);
	}
	
	public void setUniqueAttributes(Map<String, String> uniqueAttributes) {
		this.uniqueAttributes = uniqueAttributes;
	}

	public void addUniqueAttributes(String key, String value) {
		if(this.uniqueAttributes.containsKey(key)) {
			String oldXml = "<" + key + ">" + this.uniqueAttributes.get(key) + "</" + key + ">";
			String newXml = "<" + key + ">" + value + "</" + key + ">";
			this.associatedXml = this.associatedXml.replace(oldXml, newXml);
		}
		
		this.uniqueAttributes.put(key, value);
	}
	
	public void addResourceModelAttributes(String resourceModelId) {
		this.resourceModelAttributes.add(resourceModelId);
	}
	
	public List<String> getResourceModelAttributes() {
		return this.resourceModelAttributes;
	}
	
	public void setAssociatedXml(String associatedXml) {
		this.associatedXml = associatedXml;
	}

	public String getAssociatedXml() {
		return associatedXml;
	}

	public void setInitialXml(String initialXml) {
		this.initialXml = initialXml;
	}

	public String getInitialXml() {
		return initialXml;
	}

	public boolean isAdded() {
		return added;
	}

	public void setAdded(boolean added) {
		this.added = added;
	}

	public boolean isMergeable() {
		return mergeable;
	}

	public void setMergeable(boolean mergeable) {
		this.mergeable = mergeable;
	}

	public boolean isReplaceable() {
		return replaceable;
	}

	public void setReplaceable(boolean replaceable) {
		this.replaceable = replaceable;
	}

	public boolean isEditableName() {
		return editableName;
	}

	public void setEditableName(boolean editableName) {
		this.editableName = editableName;
	}

	public String getParentTag() {
		return parentTag;
	}

	public void setParentTag(String parentTag) {
		this.parentTag = "<" + parentTag + ">";
	}
}
