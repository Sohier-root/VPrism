package com.stilog.prism.vpimodel.utils;

public class VPIConstants {

	public static final String FILENAME_RESOURCE_MODEL = "resourcemodel.txt";
	public static final String FILENAME_RESOURCES_FILTER = "eventresourcefilter.txt";
	public static final String FILENAME_EVENTS_FILTER = "planningeventfilter.txt";
	public static final String FILENAME_RESOURCES_EXPORT = "exporteventresourcecontext.txt";
	public static final String FILENAME_EVENTS_EXPORT = "exporteventcontext.txt";
	public static final String FILENAME_RESOURCES_IMPORT = "importeventresourcecontext.txt";
	public static final String FILENAME_EVENTS_IMPORT = "importeventcontext.txt";
	public static final String FILENAME_EVENTS_STRUCT = "eventtreestruct.txt";
	public static final String FILENAME_HISTORY = "history.txt";
	public static final String FILENAME_HISTORY_TRACKER = "historytracker.txt";
	public static final String FILENAME_FORM_MODEL = "formmodel.txt";


	public static final String XML_TAG_FILTER_RESOURCE = "filterResource";
	public static final String XML_TAG_FILTER_EVENT = "filterEvent";

	public static final String PARAMETER_NAME = "Nom";
	public static final String PARAMETER_RESOURCEMODEL = "Dimension";
	public static final String PARAMETER_TYPE = "Type";
	public static final String PARAMETER_KEY = "Cle";
	public static final String PARAMETER_COMMENTS = "Comments";
	public static final String PARAMETER_KEY_PARENT = "Cle parent";
	public static final String PARAMETER_KEYHEADINGS = "Rubrique d'identification";
	public static final String PARAMETER_LABELSHEADINGS = "Rubriques d'affichage";
	public static final String PARAMETER_MANDATORY = "Rubriques Obligatoire";
	public static final String PARAMETER_CONDITIONS = "Conditions";
	public static final String PARAMETER_CORRESPONDANCE = "Correspondances";
	public static final String CORRESPONDANCE_DIM_SUFFIX = "__DIM";
	public static final String PARAMETER_SOURCE = "Source";
	public static final String PARAMETER_ENCODING = "Encoding";
	public static final String PARAMETER_FORMAT = "Format";
	public static final String PARAMETER_SEPARATOR = "Separateur";
	public static final String PARAMETER_PARAMETRE = "Parametres";
	public static final String PARAMETER_IMPORT_MODE = "Mode d'import";
	public static final String PARAMETER_DATE_FORMAT = "Format de date";
	public static final String PARAMETER_ACTIVATE_EVT_COM = "Commentaire pour evt";
	public static final String PARAMETER_ACTIVATE_COM = "Commentaire";
	public static final String PARAMETER_ACTIVATE_EVT_HIST = "Historique pour evt";
	public static final String PARAMETER_ACTIVATE_HIST = "Historique";
	public static final String PARAMETER_AUTO_FILTER = "Filtre rapide";
	public static final String PARAMETER_BUFFERED = "Charger tout";
	public static final String PARAMETER_CREATE_EVT = "Creation event auto";
	public static final String PARAMETER_TREESTRUCT = "Hierarchie d'événement";
	public static final String PARAMETER_CALENDAR = "Calendrier";
	public static final String PARAMETER_CREATION_RULE = "Règle de création";
	public static final String PARAMETER_COLOR = "Couleur";
	public static final String PARAMETER_VALUE_LIST = "Liste des valeurs";
	public static final String PARAMETER_FORM_MODEL = "Formulaire";
	/** Attribut caché : dimensions référencées par une condition de filtre (pour le diagramme de dépendances). */
	public static final String PARAMETER_REF_DIMENSIONS = "Dimensions référencées";

	public static final String NAME_TREE_RESOURCESMODEL = "Resources Model";
	public static final String NAME_TREE_RESOURCESFILTER = "Resources Filter";
	public static final String NAME_TREE_EVENTSFILTER = "Events Filter";
	public static final String NAME_TREE_EVENTSEXPORT = "Events Export";
	public static final String NAME_TREE_RESOURCESEXPORT = "Resources Export";
	public static final String NAME_TREE_EVENTSIMPORT = "Events Import";
	public static final String NAME_TREE_RESOURCESIMPORT = "Resources Import";
	public static final String NAME_TREE_EVENTSSTRUCT = "Hierarchies";
	public static final String NAME_TREE_FORMMODEL = "Formulaires";
}
