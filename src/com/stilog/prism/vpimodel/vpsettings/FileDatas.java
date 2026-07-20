package com.stilog.prism.vpimodel.vpsettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.Resolveable;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.VpiPlanning;

public abstract class FileDatas extends Resolveable{

	private static final String SEPARATOR = ";";

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

	            this.addEntity(new Entity(id, name));
	        }

	        for(Entity entity : this.entities) {
	            entity.setParameters(this.buildParameters(entity));
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	protected abstract List<Parameters> buildParameters(Entity entity);

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
