package com.stilog.analysevpi.model.objects;

public class Attribute {

	private String key;
	private String value;
	
	private boolean anomaly = false;
	
	public Attribute(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isAnomaly() {
		return anomaly;
	}

	public void setAnomaly(boolean anomaly) {
		this.anomaly = anomaly;
	}

	@Override
	public String toString() {
		return key + " -> " + value;
	}
	
}
