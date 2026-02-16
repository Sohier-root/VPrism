package com.stilog.vpimodel.objects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.stilog.analysevpi.utils.Compressor;
import com.stilog.analysevpi.utils.Decompressor;
import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.vpimodel.vpsettings.FileDatas;
import com.stilog.vpimodel.vpsettings.Filter;
import com.stilog.vpimodel.vpsettings.Hierarchies;
import com.stilog.vpimodel.vpsettings.ImportExport;
import com.stilog.vpimodel.vpsettings.ResourceModel;

public class VPIDatas {

	private static final String OUTPUT_DIR = System.getProperty("java.io.tmpdir") + "vpcompare/";
	private static final String OUTPUT_DIR_COMPARISON_TESTED = OUTPUT_DIR + "comparison/tested/";
	private static final String OUTPUT_DIR_COMPARISON_REF = OUTPUT_DIR + "comparison/ref/";
	private static final String OUTPUT_DIR_COMPARISON_MERGED = OUTPUT_DIR + "comparison/merged/";
	private static final String OUTPUT_DIR_DOC = OUTPUT_DIR + "documentation/";
	String name;
	String fileName;
	String filePath;
	long lenght;
	
	ResourceModel resourceModel;
	Filter resourceFilter, eventFilter;
	ImportExport exportResources, exportEvents, importResources, importEvents;
	Hierarchies hierarchies;
	
	public VPIDatas() {
		this.name = "VPI Datas";
	}
	
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
	public void computeDatas(File vpi, TypeFile type) {
		
		this.name = "VPI Datas";
		this.fileName = vpi.getName();
		this.filePath = vpi.getAbsolutePath();
		this.lenght = vpi.length();
		
		String vpiPath = vpi.getAbsolutePath();
		String outputDir = null;
		try {
			switch(type) {
			case COMPARISON_LEFT:
				outputDir = OUTPUT_DIR_COMPARISON_REF;
				break;
				
			case COMPARISON_RIGHT:
				outputDir = OUTPUT_DIR_COMPARISON_TESTED;
				break;
				
			case DOCUMENTATION:
				outputDir = OUTPUT_DIR_DOC;
				break;
				
			default:
				throw new FileNotFoundException("Le chemin de sortie n'est pas initialisé");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		Decompressor.dezipper(vpiPath, outputDir);
		
		File filesDir = new File(outputDir);
		
		/*
		 * Parse des dimensions avant tout pour correspondances
		 */
		resourceModel = new ResourceModel(filesDir.getAbsolutePath() + "/" + VPIConstants.FILENAME_RESOURCE_MODEL, VPIConstants.NAME_TREE_RESOURCESMODEL);
		resourceModel.parseDatas();
		
		for(File file : filesDir.listFiles()) {
			try {
				switch(file.getName()) {
				//Récupération des données par fichier
					/*case VPIConstants.FILENAME_RESOURCE_MODEL:
						resourceModel = new ResourceModel(file.getAbsolutePath(), VPIConstants.NAME_TREE_RESOURCESMODEL);
						resourceModel.parseDatas();
						break;*/
						
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
	
	public void generateMergedFiles(String outputPathVpi) {
		File outputDirFile = new File(OUTPUT_DIR_COMPARISON_MERGED);
		if(outputDirFile.exists())
			outputDirFile.delete();
		outputDirFile.mkdir();
		
		try {
			List<FileDatas> refFiles = this.getFilesDatas();
			for(int i = 0; i<refFiles.size(); i++) {
				refFiles.get(i).generateFile(OUTPUT_DIR_COMPARISON_MERGED);
			}
			
			File vpiFileDir = new File(OUTPUT_DIR_COMPARISON_TESTED);
			for(File vpiFile : vpiFileDir.listFiles()) {
				File mergedFile = new File(OUTPUT_DIR_COMPARISON_MERGED + vpiFile.getName());
				if(!mergedFile.exists()) {
					Files.copy(Path.of(vpiFile.toURI()), new FileOutputStream(mergedFile));
				}
			}
			
			File destFile = new File(outputPathVpi + "\\" + this.fileName.replace(".vpi", "_merged.vpi").replace(".vps", "_merged.vps"));
			Compressor.zip(outputDirFile, destFile);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Remet a zéro les données de comparaison
	 */
	public void reset() {
		for(FileDatas datas : this.getFilesDatas()) {
			datas.reset();
		}
	}
	
	public List<FileDatas> getFilesDatas() {
	    List<FileDatas> result = new ArrayList<>();

	    Class<?> clazz = this.getClass();

	    for (Field field : clazz.getDeclaredFields()) {
	    	if (FileDatas.class.isAssignableFrom(field.getType())) {
	    		try {
	    		field.setAccessible(true);
	    	    result.add((FileDatas) field.get(this));
	    		}
	    		catch(Exception e) {e.printStackTrace();}
	    	}
	    }

	    return result;
	}
	
	public void mergeParameter(FileDatas file, Entity entity, Parameters parameter, Parameters parameterToReplace) throws Exception {	
		this.getFileDatasFromThis(file).mergeParameter(entity, parameter, parameterToReplace);
	}
	
	public void mergeEntity(FileDatas file, Entity entity, Entity entityToReplace) throws Exception {
		this.getFileDatasFromThis(file).mergeEntity(entity, entityToReplace);
	}
	
	private FileDatas getFileDatasFromThis(FileDatas file){
		switch(file.getFileName()) {
		case VPIConstants.FILENAME_RESOURCE_MODEL:
			return resourceModel;
			
		case VPIConstants.FILENAME_RESOURCES_FILTER:
			return resourceFilter;
			
		case VPIConstants.FILENAME_EVENTS_FILTER:
			return eventFilter;
			
		case VPIConstants.FILENAME_RESOURCES_EXPORT:
			return exportResources;
			
		case VPIConstants.FILENAME_EVENTS_EXPORT:
			return exportEvents;
			
		case VPIConstants.FILENAME_RESOURCES_IMPORT:
			return importResources;
			
		case VPIConstants.FILENAME_EVENTS_IMPORT:
			return importEvents;
			
		case VPIConstants.FILENAME_EVENTS_STRUCT:
			return hierarchies;
			
		default:
			return null;
		}
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
		builder.append(resourceModel != null?resourceModel.toString():"");
		return builder.toString();
	}

	public boolean isParsed() {
		return fileName != null && !fileName.isBlank();
	}
}
