package com.stilog.vpimodel.objects;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.stilog.analysevpi.utils.VPIConstants;

public class Parameters extends Mergeable{

	private String name;
	private String uid;
	private List<Attribute> attributes = new ArrayList<>();
	private List<Attribute> hiddenAttributes = new ArrayList<>();
	
	public Parameters(String name) {
		this.name = name;
	}
	
	public Parameters(String name, String xml) {
		this.name = name;
		this.setInitialXml(xml);
		this.setAssociatedXml(xml);
	}
	/*
	 * METHODES
	 */
	@Override
	public void reset() {
		super.reset();
		
		for(Attribute attr : attributes) {
			attr.reset();
		}
	}
	
	/*
	 * GETTER & SETTER
	 */
	
	public String getAttributeValue(String key) {
		for(Attribute attr : attributes) {
			if(attr.getKey().equals(key))
				return attr.getValue();
		}
		
		for(Attribute attr : hiddenAttributes) {
			if(attr.getKey().equals(key))
				return attr.getValue();
		}
		
		return "";
	}

	public void setResolve(boolean resolve) {
		super.setResolve(resolve);
		
		for(Attribute attr : this.attributes) {
			attr.setResolve(true);
		}
	}

	public String getName() {
		return name;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}
	
	public void addAttributes(String key, String value) {
		this.attributes.add(new Attribute(key, value));
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}
	
	public void addHiddenAttributes(String key, String value) {
		this.hiddenAttributes.add(new Attribute(key, value));
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void sort() {
		attributes.sort(Comparator.comparing(Attribute::getKey));
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parameters other = (Parameters) obj;
		return Objects.equals(name, other.name);
	}
	
	public String getInfo() {
		String sep = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		builder.append("\t- Name : " + this.name + sep);
		builder.append("\t- xml : " + this.getAssociatedXml() + sep);
		
		return builder.toString();
	}

	@Override
	public String toString() {
		return "Parameters [name=" + name + "]";
	}
}
