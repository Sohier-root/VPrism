package com.stilog.prism.vpimodel.objects;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.stilog.prism.analyzevpi.model.history.VpsLabelResolver;
import com.stilog.prism.comparevpi.utils.Decompressor;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;
import com.stilog.prism.vpimodel.vpsettings.Filter;
import com.stilog.prism.vpimodel.vpsettings.Hierarchies;
import com.stilog.prism.vpimodel.vpsettings.ImportExport;
import com.stilog.prism.vpimodel.vpsettings.FormModel;
import com.stilog.prism.vpimodel.vpsettings.ResourceModel;
import com.stilog.prism.vpimodel.reader.VpiReaderService;
import com.visualplanning.vpi.exception.VpiException;
import com.visualplanning.vpi.model.VpiPlanning;

public class VPIDatas {

	private static final String OUTPUT_DIR = new File(System.getProperty("java.io.tmpdir"), "vpcompare").getAbsolutePath() + File.separator;
	private static final String OUTPUT_DIR_COMPARISON_TESTED = OUTPUT_DIR + "comparison/tested/";
	private static final String OUTPUT_DIR_COMPARISON_REF    = OUTPUT_DIR + "comparison/ref/";
	private static final String OUTPUT_DIR_DOC               = OUTPUT_DIR + "documentation/";
	/** Répertoire dédié au Module 2 (VPI unique). */
	private static final String OUTPUT_DIR_SINGLE            = OUTPUT_DIR + "single/";

	String name;
	String fileName;
	String filePath;
	long lenght;
	
	ResourceModel resourceModel;
	FormModel formModel;
	Filter resourceFilter, eventFilter;
	ImportExport exportResources, exportEvents, importResources, importEvents;
	Hierarchies hierarchies;

	/**
	 * Résolveur de labels de ressources (id → nom), coûteux à charger (scan complet
	 * de l'archive) : chargé paresseusement au premier appel de
	 * {@link #getResourceLabelResolver()}, uniquement quand l'affichage d'un filtre
	 * en a réellement besoin (voir Filter.resolverSupplier).
	 */
	private VpsLabelResolver resourceLabelResolver;

	public VPIDatas() {
		this.name = "VPI Datas";
	}
	
	/*
	 * GETTER & SETTER
	 */
	public ResourceModel getResourcesModel() {
		return resourceModel;
	}
	
	public FormModel getFormModel() {
		return formModel;
	}
	
	public Filter getResourceFilter() {
		return resourceFilter;
	}

	public Filter getEventFilter() {
		return eventFilter;
	}

	/**
	 * Charge (au premier appel seulement, puis met en cache) le résolveur de labels
	 * de ressources pour ce fichier. Ne rien appeler ici tant qu'aucun affichage n'en
	 * a besoin : c'est un scan complet de l'archive, coûteux sur de gros fichiers.
	 */
	public VpsLabelResolver getResourceLabelResolver() {
		if (resourceLabelResolver == null) {
			resourceLabelResolver = new VpsLabelResolver();
			try {
				resourceLabelResolver.load(new File(filePath));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return resourceLabelResolver;
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

			case SINGLE:
				outputDir = OUTPUT_DIR_SINGLE;
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
		 * Parse VPIReader du même fichier .vpi/.vps (en mémoire, sans passer par filesDir) :
		 * fournit le graphe typé et déjà résolu que les FileDatas migrées consomment dans
		 * leur buildParameters(Entity) à la place du DOM-walk manuel.
		 *
		 * En cas d'échec (fichier corrompu, format non supporté), on interrompt
		 * immédiatement plutôt que de continuer avec planning=null : sans ça, les
		 * FileDatas migrées échouaient plus loin avec une NullPointerException
		 * cryptique, invisible pour l'utilisateur (voir LoadingWindow.run).
		 */
		VpiPlanning planning;
		try {
			planning = VpiReaderService.parse(vpi);
		}
		catch (VpiException e) {
			throw new IllegalStateException(
				"Impossible de lire le fichier VPI/VPS \"" + vpi.getName() + "\" : " + e.getMessage(), e);
		}

		//Dimension
		resourceModel = new ResourceModel(filesDir.getAbsolutePath() + "/" + VPIConstants.FILENAME_RESOURCE_MODEL, VPIConstants.NAME_TREE_RESOURCESMODEL);
		resourceModel.setPlanning(planning);
		resourceModel.parseDatas();

		//Formulaires
		formModel = new FormModel(filesDir.getAbsolutePath() + "/" + VPIConstants.FILENAME_FORM_MODEL, VPIConstants.NAME_TREE_FORMMODEL);
		formModel.setPlanning(planning);
		formModel.parseDatas();

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
						resourceFilter.setFilterKind(VPIConstants.XML_TAG_FILTER_RESOURCE);
						resourceFilter.setPlanning(planning);
						resourceFilter.setResolverSupplier(this::getResourceLabelResolver);
						resourceFilter.parseDatas();
						break;

					case VPIConstants.FILENAME_EVENTS_FILTER:
						eventFilter = new Filter(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSFILTER);
						eventFilter.setFilterKind(VPIConstants.XML_TAG_FILTER_EVENT);
						eventFilter.setPlanning(planning);
						eventFilter.setResolverSupplier(this::getResourceLabelResolver);
						eventFilter.parseDatas();
						break;

					case VPIConstants.FILENAME_RESOURCES_EXPORT:
						exportResources = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_RESOURCESEXPORT);
						exportResources.setPlanning(planning);
						exportResources.setContexts(planning != null ? planning.getImportExportSet().getExportEventResourceContexts() : null);
						exportResources.parseDatas();
						break;

					case VPIConstants.FILENAME_EVENTS_EXPORT:
						exportEvents = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSEXPORT);
						exportEvents.setPlanning(planning);
						exportEvents.setContexts(planning != null ? planning.getImportExportSet().getExportEventContexts() : null);
						exportEvents.parseDatas();
						break;

					case VPIConstants.FILENAME_RESOURCES_IMPORT:
						importResources = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_RESOURCESIMPORT);
						importResources.setImport(true);
						importResources.setPlanning(planning);
						importResources.setContexts(planning != null ? planning.getImportExportSet().getImportEventResourceContexts() : null);
						importResources.parseDatas();
						break;

					case VPIConstants.FILENAME_EVENTS_IMPORT:
						importEvents = new ImportExport(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSIMPORT);
						importEvents.setImport(true);
						importEvents.setPlanning(planning);
						importEvents.setContexts(planning != null ? planning.getImportExportSet().getImportEventContexts() : null);
						importEvents.parseDatas();
						break;

					case VPIConstants.FILENAME_EVENTS_STRUCT:
						hierarchies = new Hierarchies(file.getAbsolutePath(), VPIConstants.NAME_TREE_EVENTSSTRUCT);
						hierarchies.setPlanning(planning);
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
	    		FileDatas fd = (FileDatas) field.get(this);
	    		if (fd != null && !fd.isHidden())
	    		    result.add(fd);
	    		}
	    		catch(Exception e) {e.printStackTrace();}
	    	}
	    }

	    return result;
	}
	
	public String getName() {
		return name;
	}

	public String getFilePath() {
		return filePath;
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
