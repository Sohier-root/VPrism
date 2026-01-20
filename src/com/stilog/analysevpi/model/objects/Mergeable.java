package com.stilog.analysevpi.model.objects;

import java.util.HashMap;
import java.util.Map;

public abstract class Mergeable {

	private Map<String, String> uniqueAttributes = new HashMap<>();
	private String associatedXml;
	
	public Map<String, String> getUniqueAttributes() {
		return uniqueAttributes;
	}
	
	public String getUniqueAttribute(String name) {
		return uniqueAttributes.get(name);
	}
	
	public void addUniqueAttributes(String key, String value) {
		if(this.uniqueAttributes.containsKey(key)) {
			String oldXml = "<" + key + ">" + this.uniqueAttributes.get(key) + "</" + key + ">";
			String newXml = "<" + key + ">" + value + "</" + key + ">";
			this.associatedXml = this.associatedXml.replace(oldXml, newXml);
		}
		
		this.uniqueAttributes.put(key, value);
	}
	
	public void setAssociatedXml(String associatedXml) {
		this.associatedXml = associatedXml;
	}

	public String getAssociatedXml() {
		return associatedXml;
	}
}
