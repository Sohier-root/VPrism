package com.stilog.analysevpi.model;

import java.util.HashMap;
import java.util.Map;

public class Correspondances {

	Map<String, String> map = new HashMap<>();

	public Map<String, String> getMap() {
		return map;
	}
	
	public void addCorr(String key, String value) {
		this.map.putIfAbsent(key, value);
	}
	
	public String getCorrespondance(String key) {
		return map.containsKey(key)?map.get(key):"";
	}
}
