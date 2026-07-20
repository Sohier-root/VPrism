package com.stilog.prism.vpimodel.vpsettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.module.ResolutionException;
import java.nio.charset.Charset;
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

import com.stilog.prism.comparevpi.model.GeneralCorrespondance;
import com.stilog.prism.comparevpi.utils.GUID;
import com.stilog.prism.comparevpi.utils.MethodUtil;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Mergeable;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.Resolveable;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.VpiPlanning;

public abstract class FileDatas extends Resolveable{

	private static final String SEPARATOR = ";";
	private static final String LINE_SEPARATOR = System.lineSeparator();
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	private static final DocumentBuilderFactory DB_FACTORY = DocumentBuilderFactory.newInstance();

	String name;
	File file;
	List<Entity> entities = new ArrayList<>();

	private boolean hidden = false;

	/**
	 * Graphe VPIReader déjà résolu, fourni par VPIDatas aux sous-classes migrées
	 * pour qu'elles y puisent les valeurs au lieu de re-parser le XML brut.
	 * Reste {@code null} pour les classes non migrées (ex. CreationRule).
	 */
	protected VpiPlanning planning;

	public void setPlanning(VpiPlanning planning) {
		this.planning = planning;
	}
	
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

	    try (BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
	        while ((line = br.readLine()) != null) {
	            String[] values = line.split(SEPARATOR);
	            int id = Integer.parseInt(values[0]);
	            String name = values[1].replace("\"", "");
	            
	            String xml = MethodUtil.decodeBase64(values[3].replace("\"", ""));
	            Document doc = getDocument(xml);
	            String xmlFormat = XML_HEADER + LINE_SEPARATOR + MethodUtil.nodeToString(doc);
	            
	            Entity entity = new Entity(id, name);
	            entity.setInitialXml(xmlFormat);
	            entity.setAssociatedXml(xmlFormat);
	            
	            this.addEntity(entity);
	        }
	        
	        for(Entity entity : this.entities) {
	            entity.setParameters(this.parseXml(entity));
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public void mergeParameter(Entity entity, Parameters parameter, Parameters parameterToReplace) throws Exception  {
		Entity targetEntity = this.getEntity(entity.getName());
		
		if(parameterToReplace != null) {
			if(!parameterToReplace.isEditableName() && !parameter.getName().equals(parameterToReplace.getName()))
				throw new Exception("Le nom doit être identique pour remplacer ces données!");
			parameter.setUniqueAttributes(parameterToReplace.getUniqueAttributes());
			parameter.setInitialXml(parameterToReplace.getInitialXml());
			targetEntity.removeParameter(parameterToReplace);
		}
		else {
			parameter = (Parameters) makeUnique(parameter);
		}
		
		parameter.setResolve(true);
		targetEntity.addParameter(parameter);
	}
	
	public void mergeEntity(Entity entity, Entity entityToReplace) throws Exception {
		
		if(entityToReplace != null) {
			if(!entityToReplace.isEditableName() && !entity.getName().equals(entityToReplace.getName()))
				throw new Exception("Le nom doit être identique pour remplacer ces données!");
			entity.setUniqueAttributes(entityToReplace.getUniqueAttributes());
			this.entities.remove(entityToReplace);
		}
		else {
			entity = (Entity) makeUnique(entity);
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
		
		obj.setAdded(true);
		return obj;
	}

	
	protected Document getDocument(String xml) {
		try {
			DocumentBuilder dBuilder = DB_FACTORY.newDocumentBuilder();
			
			InputSource is = new InputSource(new StringReader(xml));
			return dBuilder.parse(is);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected abstract List<Parameters> parseXml(Entity entity);
	
	/*
	 * Regénère le fichier avec les modification potentielle
	 */
	public void generateFile(String outputDir) {
		try {
			File newFile = new File(outputDir, this.getFileName());
			newFile.createNewFile();
			FileWriter writer = new FileWriter(newFile, Charset.forName("UTF-8"));
			for(Entity ent :  this.entities) {
				String xmlEncode = "\"" + ent.generateXml() + "\"";
				String name = "\"" + ent.getName() + "\"";
				writer.append(ent.getId() + SEPARATOR + name + SEPARATOR + "0" + SEPARATOR +  xmlEncode + LINE_SEPARATOR);
			}
			writer.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void reset() {
		super.reset();
		for(Entity ent : this.entities) {
			ent.reset();
		}
	}

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
	
	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public String toString() {
		return "FileDatas [name=" + name + "]";
	}
	
}
