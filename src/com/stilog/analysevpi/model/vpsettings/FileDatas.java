package com.stilog.analysevpi.model.vpsettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.utils.VPIConstants;

public abstract class FileDatas {
	
	String name;
	File file;
	List<Entity> entities = new ArrayList<>();
	
	boolean anomaly = false;
	
	public FileDatas(String filePath, String name) {
		super();
		this.file = new File(filePath);
		this.name = name;
	}
	
	/*
	 * GETTER & SETTER
	 */
	public void addEntity(Entity entity) {
		this.entities.add(entity);
	}
	
	public List<Entity> getEntities() {
		return entities;
	}
	
	public Entity getEntity(String name) {
		for(Entity entity : entities) {
			if(entity.getName().equals(name))
				return entity;
		}
		
		return null;
	}

	public String getFileName() {
		return file.getName();
	}
	
	public boolean isAnomaly() {
		return anomaly;
	}

	public void setAnomaly(boolean anomaly) {
		this.anomaly = anomaly;
	}

	public String getName() {
		return name;
	}

	public boolean isExcludedTag(String tagName){
		return List.of(VPIConstants.XML_TAG_ID, VPIConstants.XML_TAG_URL).contains(tagName);
	}
	/*
	 * Methodes
	 */
	public void parseDatas() {
		String line = "";
	    String separator = ";"; // ou ; selon ton csv

	    try (BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
	        while ((line = br.readLine()) != null) {
	            String[] values = line.split(separator);
	            int id = Integer.parseInt(values[0]);
	            String name = values[1].replace("\"", "");
	            
	            GeneralCorrespondance.getInstance().addresourceModel(values[0], name);
	            String xml = decodeBase64(values[3].replace("\"", ""));
	            
	            Entity entity = new Entity(id, name);
	            entity.setAssociatedXml(xml);
	            
	            this.addEntity(entity);
	        }
	        
	        for(Entity entity : this.entities) {
	            entity.setParameters(this.parseXml(entity.getAssociatedXml()));
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public static String decodeBase64(String encoded) {
	    try {
	        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
	        return new String(decodedBytes, StandardCharsets.UTF_8);
	    } catch (IllegalArgumentException e) {
	    	e.printStackTrace();
	        return null; // ou throw new RuntimeException("Base64 invalide");
	    }
	}
	
	protected Document getDocument(String xml) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			InputSource is = new InputSource(new StringReader(xml));
			return dBuilder.parse(is);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected abstract List<Parameters> parseXml(String xml);

	public void sort() {
		entities.sort(Comparator.comparing(Entity::getName));
		
		for(Entity entity : entities) {
			entity.sort();
		}
	}
	
	public String getInfo() {
		String sep = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		
		builder.append("Nom du fichier : " + this.getFileName() + sep);
		
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return "FileDatas [name=" + name + "]";
	}
	
}
