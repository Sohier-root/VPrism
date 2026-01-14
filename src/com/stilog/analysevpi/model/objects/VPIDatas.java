package com.stilog.analysevpi.model.objects;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.stilog.analysevpi.model.vpsettings.FileDatas;
import com.stilog.analysevpi.model.vpsettings.Filter;
import com.stilog.analysevpi.model.vpsettings.Hierarchies;
import com.stilog.analysevpi.model.vpsettings.ImportExport;
import com.stilog.analysevpi.model.vpsettings.ResourceModel;
import com.stilog.analysevpi.utils.Decompressor;
import com.stilog.analysevpi.utils.VPIConstants;

public class VPIDatas {

	String name;
	String fileName;
	String filePath;
	long lenght;
	
	ResourceModel resourceModel;
	Filter resourceFilter, eventFilter;
	ImportExport exportResources, exportEvents, importResources, importEvents;
	Hierarchies hierarchies;
	
	/*
	 * GETTER & SETTER
	 */
	public ResourceModel getResourcesModel() {
		return resourceModel;
	}
	
	public Filter getResourceFilter() {
		return resourceFilter;
	}

	public Filter getEventFilter() {
		return eventFilter;
	}

	/*
	 * METHODS
	 */
	public void computeDatas(File vpi) {
		
		this.name = "VPI Datas";
		this.fileName = vpi.getName();
		this.filePath = vpi.getAbsolutePath();
		this.lenght = vpi.length();
		
		String vpiPath = vpi.getAbsolutePath();
		String outputDir = "tmp/dezipVPI/";
		Decompressor.dezipper(vpiPath, outputDir);
		
		File filesDir = new File(outputDir);
		for(File file : filesDir.listFiles()) {
			try {
				switch(file.getName()) {
				//Récupération des données par fichier
					case VPIConstants.FILENAME_RESOURCE_MODEL:
						resourceModel = new ResourceModel(file.getAbsolutePath(), VPIConstants.NAME_TREE_RESOURCESMODEL);
						resourceModel.parseDatas();
						break;
						
					case VPIConstants.FILENAME_RESOURCES_FILTER:
						resourceFilter = new Filter(file.getAbsolutePath(), VPIConstants.NAME_TREE_RESOURCESFILTER);
						resourceFilter.parseDatas();
						break;
						
					case VPIConstants.FILENAME_EVENTS_FILTER:
						eventFilter = new Filter(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSFILTER);
						eventFilter.parseDatas();
						break;
						
					case VPIConstants.FILENAME_RESOURCES_EXPORT:
						exportResources = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_RESOURCESEXPORT);
						exportResources.parseDatas();
						break;
						
					case VPIConstants.FILENAME_EVENTS_EXPORT:
						exportEvents = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSEXPORT);
						exportEvents.parseDatas();
						break;
						
					case VPIConstants.FILENAME_RESOURCES_IMPORT:
						importResources = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_RESOURCESIMPORT);
						importResources.setImport(true);
						importResources.parseDatas();
						break;
						
					case VPIConstants.FILENAME_EVENTS_IMPORT:
						importEvents = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSIMPORT);
						importEvents.setImport(true);
						importEvents.parseDatas();
						break;
						
					case VPIConstants.FILENAME_EVENTS_STRUCT:
						hierarchies = new Hierarchies(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSSTRUCT);
						hierarchies.parseDatas();
						break;
				}
			}
			catch(Exception e) {
				System.out.println("Erreur lors du parsing de " + file.getName());
				e.printStackTrace();
			}
		}
		
	}

	public List<Field> getFilesDatas() {
	    List<Field> result = new ArrayList<>();

	    Class<?> clazz = this.getClass();

	    for (Field field : clazz.getDeclaredFields()) {
	    	if (FileDatas.class.isAssignableFrom(field.getType())) {
	    		field.setAccessible(true);
	    	    result.add(field);
	    	}
	    }

	    return result;
	}
	
	public String getName() {
		return name;
	}

	public String getInfo() {
		String sep = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		
		builder.append("Nom de l'archive : " + this.fileName + sep);
		builder.append("Localisation : " + this.filePath + sep);
		
		return builder.toString();
	}
	
	@Override
	public String toString() {
		String sep = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		builder.append("VPIDatas [fileName=" + fileName + ", lenght=" + lenght + "]" + sep);
		builder.append(resourceModel.toString());
		return builder.toString();
	}
}
