package com.stilog.analysevpi.model.objects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Entity {

	private int uid;
	private String name;
	List<Parameters> parameters = new ArrayList<>();
	private String associatedXml;
	
	private boolean anomaly = false;
	
	public Entity(int id, String name) {
		this.uid = id;
		this.name = name;
	}
	
	/*
	 * GETTER & SETTER
	 */
	public boolean isAnomaly() {
		return anomaly;
	}

	public void setAnomaly(boolean anomaly) {
		this.anomaly = anomaly;
	}
	
	public void setParameters(List<Parameters> parameters) {
		this.parameters = parameters;
	}
	
	public Parameters getParameter(String name) {
		for(Parameters param : parameters) {
			if(param.getName().equals(name))
				return param;
		}
		
		return null;
	}
	
	public List<Parameters> getParameters(String name) {
		List<Parameters> result = new ArrayList<>();
		for(Parameters param : parameters) {
			if(param.getName().equals(name))
				result.add(param);
		}
		
		return result;
	}

	public void setAssociatedXml(String associatedXml) {
		this.associatedXml = associatedXml;
	}

	public String getAssociatedXml() {
		return associatedXml;
	}

	public int getId() {
		return uid;
	}

	public String getName() {
		return name;
	}

	public List<Parameters> getParameters() {
		return parameters;
	}

	/*
	 * METHODS
	 */
	/*@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Entity [name=" + name + "]" + System.lineSeparator());
		for(Parameters param : parameters)
			builder.append(param.toString() + System.lineSeparator());
		return builder.toString();
	}*/
	
	public void sort() {
		parameters.sort(Comparator.comparing(Parameters::getName));
		
		for(Parameters attr : parameters) {
			attr.sort();
		}
	}
	
	public String getInfo() {
		String sep = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		builder.append("Name : " + this.name + sep);
		builder.append("Parameters : " + sep);
		for(Parameters param : parameters) {
			builder.append( param.getInfo() + sep);
		}
		//builder.append("XML : "+ sep + this.associatedXml + sep);
		
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return "Entity [name=" + name + "]";
	}
	
}
