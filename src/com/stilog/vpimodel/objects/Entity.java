package com.stilog.vpimodel.objects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.stilog.analysevpi.utils.MethodUtil;
import com.stilog.analysevpi.utils.VPIConstants;

public class Entity extends Mergeable{

	private int id;
	private String name;
	List<Parameters> parameters = new ArrayList<>();

	private List<Attribute> hiddenAttributes = new ArrayList<>();
	
	private TypeData typeData;
	
	public Entity(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	/*
	 * GETTER & SETTER
	 */

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
	
	public void addParameter(Parameters param) {
		this.parameters.add(param);
	}
	
	public void removeParameter(Parameters param) {
		this.parameters.remove(param);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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
	
	public String generateXml() {
		for(String key : this.getUniqueAttributes().keySet()) {
			String tagToReplace = "<" + key + ">*</" + key + ">";
			String newTag = "<" + key + ">" + this.getUniqueAttribute(key) + "</" + key + ">";
			this.setAssociatedXml(this.getAssociatedXml().replaceFirst(tagToReplace, newTag));
			if(key.equals(VPIConstants.XML_TAG_ID))
				this.setId(Integer.parseInt(this.getUniqueAttribute(key)));
		}
		
		for(Parameters param : this.parameters) {
			if(param.getInitialXml() == null)
				continue;
			//Si le paramètre a été ajouté 
			if(param.isAdded()) {
				String concatXml = param.getParentTag() + System.lineSeparator() + param.getAssociatedXml();
				this.setAssociatedXml(this.getAssociatedXml().replace(param.getParentTag(), concatXml));
			}
			//Sinon, le paramètre a été remplacé
			else {
				this.setAssociatedXml(this.getAssociatedXml().replace(param.getInitialXml(), param.getAssociatedXml()));
			}
		}
		
		return MethodUtil.encodeBase64(this.getAssociatedXml());
	}
	
	@Override
	public void reset() {
		super.reset();
		
		for(Parameters param : this.parameters) {
			param.reset();
		}
	}
	
	public void sort() {
		if(parameters == null)
			return;
		
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
	
	public TypeData getTypeData() {
		return typeData;
	}

	public void setTypeData(TypeData typeData) {
		this.typeData = typeData;
	}
	
	public void addHiddenAttributes(String key, String value) {
		this.hiddenAttributes.add(new Attribute(key, value));
	}
	
	public String getHiddenAttributeValue(String key) {
		for(Attribute attr : hiddenAttributes) {
			if(attr.getKey().equals(key))
				return attr.getValue();
		}
		
		return "";
	}
	
	@Override
	public String toString() {
		return "Entity [name=" + name + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		return Objects.equals(name, other.name);
	}
	
}
