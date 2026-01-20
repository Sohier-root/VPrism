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
import com.stilog.analysevpi.model.objects.Mergeable;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.utils.GUID;
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
	            
	            String xml = decodeBase64(values[3].replace("\"", ""));
	            
	            Entity entity = new Entity(id, name);
	            entity.setAssociatedXml(xml);
	            
	            this.addEntity(entity);
	        }
	        
	        for(Entity entity : this.entities) {
	            entity.setParameters(this.parseXml(entity));
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public void mergeParameter(Entity entity, Parameters parameter, Parameters parameterToReplace) {
		Entity targetEntity = this.getEntity(entity.getName());
		
		parameter = (Parameters) makeUnique(parameter);
		
		if(parameterToReplace != null) {
			targetEntity.removeParameter(parameterToReplace);
		}
		
		parameter.setResolve(true);
		targetEntity.addParameter(parameter);
	}
	
	public void mergeEntity(Entity entity, Entity entityToReplace) {
		entity = (Entity) makeUnique(entity);
		if(entityToReplace != null) {
			this.entities.remove(entityToReplace);
		}
		
		entity.setResolve(true);
		this.addEntity(entity);
	}
	
	private Mergeable makeUnique(Mergeable obj) {
		GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
		
		for(String uniqueAttr : obj.getUniqueAttributes().keySet()) {
			String uniqueValue = obj.getUniqueAttribute(uniqueAttr);
			//Si l'attribut unique existe deja, le rendre unique
			if(!gCorr.getCorrespondance(uniqueAttr, uniqueValue).isBlank()){
				//Gestion des différents type d'attribut unique 
				switch(uniqueAttr) {
				case VPIConstants.XML_TAG_ID:
					int newValue = Integer.parseInt(uniqueValue);
					while(!gCorr.getCorrespondance(uniqueAttr, String.valueOf(newValue)).isBlank()) {
						newValue ++;
					}
					obj.addUniqueAttributes(uniqueAttr, String.valueOf(newValue));
					break;
				case VPIConstants.XML_TAG_UID:
					String newGuid = GUID.makeGUID();
					obj.addUniqueAttributes(uniqueAttr, newGuid);
					break;
				}
			}
		}
		
		return obj;
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
	
	protected abstract List<Parameters> parseXml(Entity entity);

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
