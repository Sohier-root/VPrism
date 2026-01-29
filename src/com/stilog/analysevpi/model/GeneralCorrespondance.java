package com.stilog.analysevpi.model;

import java.util.HashMap;
import java.util.Map;

public class GeneralCorrespondance {

	private Map<String, Correspondances> corrMap = new HashMap<>();
	
	private static GeneralCorrespondance instance;
	
	public static GeneralCorrespondance getInstance() {
		if(instance == null)
			instance = new GeneralCorrespondance();
		
		return instance;
	}
	
	public void addCorrespondance(String nodeName, String key, String value) {
		Correspondances corr = null;
		if(corrMap.containsKey(nodeName)) {
			corr = corrMap.get(nodeName);
		}
		else {
			corr = new Correspondances();
		}
		corr.addCorr(key, value);
		this.corrMap.put(nodeName, corr);
	}
	
	public String getCorrespondance(String nodeName, String key) {
		if(!this.corrMap.containsKey(nodeName)) {
			return "";
		}
		return this.corrMap.get(nodeName).getCorrespondance(key);
	}
}
