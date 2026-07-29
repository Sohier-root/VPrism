package com.stilog.prism.vpimodel.vpsettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.Resolveable;
import com.stilog.prism.vpimodel.reader.PropertyLabels;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.VpiPlanning;
import com.visualplanning.vpi.model.dimension.Heading;
import com.visualplanning.vpi.model.dimension.heading.HeadingMultiChoice;
import com.visualplanning.vpi.model.dimension.heading.HeadingResourceReference;
import com.visualplanning.vpi.model.dimension.heading.HeadingUniqueChoice;
import com.visualplanning.vpi.model.property.Property;

public abstract class FileDatas extends Resolveable{

	private static final String SEPARATOR = ";";

	String name;
	File file;
	List<Entity> entities = new ArrayList<>();

	private boolean hidden = false;

	/**
	 * Graphe VPIReader déjà résolu, fourni par VPIDatas à chaque sous-classe
	 * pour qu'elle y puise les valeurs au lieu de parser du XML brut.
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

	/**
	 * Construit un {@link Parameters} par rubrique (nom, type, propriétés cachées),
	 * commun à {@link ResourceModel} et {@link FormModel} qui exposent tous deux une
	 * {@code List<Heading>} (dimension ou formulaire).
	 */
	protected static List<Parameters> computeHeadingParameters(List<Heading> headings) {
		List<Parameters> result = new ArrayList<>();

		for (Heading heading : headings) {
			Parameters param = new Parameters(heading.getName());
			param.setUid(heading.getUid());
			param.addAttributes(VPIConstants.PARAMETER_NAME, heading.getName());
			param.addAttributes(VPIConstants.PARAMETER_TYPE, PropertyLabels.label(heading.getHeadingType()));

			// Propriétés déjà rendues sous forme d'attribut visible : à exclure des attributs cachés.
			Set<Property> alreadyShown = new HashSet<>();

			if (heading instanceof HeadingResourceReference ref) {
				alreadyShown.add(ref.getReferencedDimension());
				if (ref.getReferencedDimension().getEntityId() != -1) {
					param.addAttributes(VPIConstants.PARAMETER_RESOURCEMODEL, ref.getReferencedDimension().getDisplayValue());
				}
			}
			if (heading instanceof HeadingUniqueChoice choice) {
				alreadyShown.add(choice.getChoices());
				param.addAttributes(VPIConstants.PARAMETER_VALUE_LIST, choice.getChoices().getDisplayValue());
			}
			if (heading instanceof HeadingMultiChoice choice) {
				alreadyShown.add(choice.getChoices());
				param.addAttributes(VPIConstants.PARAMETER_VALUE_LIST, choice.getChoices().getDisplayValue());
			}

			for (Property p : heading.getProperties()) {
				if (alreadyShown.contains(p))
					continue;
				param.addHiddenAttributes(p.getLabel(), p.getDisplayValue());
			}

			result.add(param);
		}
		return result;
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
