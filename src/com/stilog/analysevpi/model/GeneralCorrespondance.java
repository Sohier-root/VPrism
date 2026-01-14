package com.stilog.analysevpi.model;

import java.util.HashMap;
import java.util.Map;

public class GeneralCorrespondance {

	private Map<String, String> rubCorr = new HashMap<>();

	private Map<String, String> resourceModelCorr = new HashMap<>();
	
	private static GeneralCorrespondance instance;
	
	public static GeneralCorrespondance getInstance() {
		if(instance == null)
			instance = new GeneralCorrespondance();
		
		return instance;
	}
	
	public void addRub(String key, String name) {
		this.rubCorr.put(key, name);
	}
	
	public String getRubName(String key) {
		return this.rubCorr.get(key);
	}
	
	public void addresourceModel(String key, String name) {
		this.rubCorr.put(key, name);
	}
	
	public String getResourceModelName(String key) {
		return this.rubCorr.get(key);
	}
}
