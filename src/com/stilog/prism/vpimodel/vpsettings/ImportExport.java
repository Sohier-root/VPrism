package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.dimension.Dimension;
import com.visualplanning.vpi.model.importexport.CsvFileConfig;
import com.visualplanning.vpi.model.importexport.Definition;
import com.visualplanning.vpi.model.importexport.ExportEventResourceContext;
import com.visualplanning.vpi.model.importexport.ImportExportContextBase;
import com.visualplanning.vpi.model.importexport.ImportExportProperty;
import com.visualplanning.vpi.model.importexport.ImportEventResourceContext;
import com.visualplanning.vpi.model.property.PropertyText;

public class ImportExport extends FileDatas{
	boolean isImport = false;

	/** Liste (parmi les 4 de {@code ImportExportSet}) correspondant à ce fichier, fournie par VPIDatas. */
	private List<? extends ImportExportContextBase> contexts;

	public ImportExport(String filePath, String name) {
		super(filePath, name);
	}

	public void setContexts(List<? extends ImportExportContextBase> contexts) {
		this.contexts = contexts;
	}

	/*
	 * PARSE (via VPIReader, sauf Cle/Cle parent : non couvert par VPIReader,
	 * conservé en lecture DOM directe sur le fragment XML de l'entité)
	 */
	@Override
	protected List<Parameters> parseXml(Entity entity) {

		List<Parameters> paramList = new ArrayList<>();
		ImportExportContextBase context = findContext(entity.getId());
		if (context == null) {
			System.out.println("Contexte import/export introuvable pour l'entité id=" + entity.getId());
			return paramList;
		}

		entity.addUniqueAttributes(VPIConstants.XML_TAG_ID, String.valueOf(context.getId()));
		entity.addUniqueAttributes(VPIConstants.XML_TAG_UID, context.getUid());
		entity.setMergeable(true);
		entity.setReplaceable(true);

		Definition def = context.getDefinition();

		paramList.add(computeCorrespondance(def));
		paramList.addAll(computeConfiguration(entity, context, def));

		return paramList;
	}

	private ImportExportContextBase findContext(int id) {
		if (contexts == null)
			return null;
		for (ImportExportContextBase c : contexts) {
			if (c.getId() == id)
				return c;
		}
		return null;
	}

	private Parameters computeCorrespondance(Definition def) {
		Parameters param = new Parameters(VPIConstants.PARAMETER_CORRESPONDANCE);
		param.setReplaceable(true);
		param.setEditableName(false);

		for (ImportExportProperty prop : def.getCorrespondences()) {
			param.addAttributes(prop.title(), prop.columnValue());

			if (prop.resourceModelId() != -1) {
				String dimName = dimensionNameById(prop.resourceModelId());
				if (dimName != null && !dimName.isBlank())
					param.addHiddenAttributes(prop.title() + VPIConstants.CORRESPONDANCE_DIM_SUFFIX, dimName);
			}
		}
		return param;
	}

	private List<Parameters> computeConfiguration(Entity entity, ImportExportContextBase context, Definition def) {
		List<Parameters> paramList = new ArrayList<>();

		/*
		 * SOURCE
		 */
		CsvFileConfig src = def.getSourceConfig();
		Parameters paramSrc = new Parameters(VPIConstants.PARAMETER_SOURCE);
		paramSrc.setReplaceable(true);
		paramSrc.setEditableName(false);
		paramSrc.addAttributes(VPIConstants.PARAMETER_FORMAT, src.format());
		paramSrc.addAttributes(VPIConstants.PARAMETER_ENCODING, src.encoding());
		paramSrc.addAttributes(VPIConstants.PARAMETER_SEPARATOR, src.separator());
		paramList.add(paramSrc);

		/*
		 * CLE / CLE PARENT — non couvert par VPIReader (Definition n'expose pas
		 * keyAttributes/parentKeyAttributes) : lu directement dans le fragment XML brut.
		 */
		List<String> keyTitles = readKeyTitles(entity.getAssociatedXml(), VPIConstants.XML_TAG_KEY_ATTRIBUTES);
		if (!keyTitles.isEmpty()) {
			Parameters paramKey = new Parameters(VPIConstants.PARAMETER_KEY);
			paramKey.setReplaceable(true);
			paramKey.setEditableName(false);
			for (int i = 0; i < keyTitles.size(); i++)
				paramKey.addAttributes(VPIConstants.PARAMETER_KEY + i, keyTitles.get(i));
			paramList.add(paramKey);
		}

		List<String> keyParentTitles = readKeyTitles(entity.getAssociatedXml(), VPIConstants.XML_TAG_PARENT_KEY_ATTRIBUTES);
		if (!keyParentTitles.isEmpty()) {
			Parameters paramKeyParent = new Parameters(VPIConstants.PARAMETER_KEY_PARENT);
			paramKeyParent.setReplaceable(true);
			paramKeyParent.setEditableName(false);
			for (int i = 0; i < keyParentTitles.size(); i++)
				paramKeyParent.addAttributes(VPIConstants.PARAMETER_KEY_PARENT + i, keyParentTitles.get(i));
			paramList.add(paramKeyParent);
		}

		/*
		 * PARAMETRE
		 */
		Parameters paramParam = new Parameters(VPIConstants.PARAMETER_PARAMETRE);
		paramParam.setReplaceable(true);
		paramParam.setEditableName(false);

		PropertyText importMode = def.getImportMode();
		if (importMode != null)
			paramParam.addAttributes(VPIConstants.PARAMETER_IMPORT_MODE, importMode.getDisplayValue());

		Integer resourceModelId = resourceModelEntityId(context);
		if (resourceModelId != null && resourceModelId != -1)
			paramParam.addAttributes(VPIConstants.PARAMETER_RESOURCEMODEL, dimensionNameById(resourceModelId));

		PropertyText dateFormat = def.getDateFormat();
		if (dateFormat != null)
			paramParam.addAttributes(VPIConstants.PARAMETER_DATE_FORMAT, dateFormat.getDisplayValue());

		paramList.add(paramParam);

		return paramList;
	}

	private Integer resourceModelEntityId(ImportExportContextBase context) {
		if (context instanceof ExportEventResourceContext c)
			return c.getResourceModelEntityId().getValue();
		if (context instanceof ImportEventResourceContext c)
			return c.getResourceModelEntityId().getValue();
		return null;
	}

	private String dimensionNameById(int id) {
		for (Dimension dim : this.planning.getDimensions()) {
			if (dim.getId() == id)
				return dim.getName().getDisplayValue();
		}
		return null;
	}

	private List<String> readKeyTitles(String entityXml, String containerTag) {
		List<String> titles = new ArrayList<>();
		try {
			Document doc = getDocument(entityXml);
			NodeList containers = doc.getElementsByTagName(containerTag);
			if (containers.getLength() == 0)
				return titles;

			NodeList children = ((Element) containers.item(0)).getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element key = (Element) child;
				NodeList titleNodes = key.getElementsByTagName(VPIConstants.XML_TAG_TITLE);
				if (titleNodes.getLength() > 0)
					titles.add(titleNodes.item(0).getTextContent());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return titles;
	}

	/*
	 * METHODS
	 */
	public boolean isImport() {
		return isImport;
	}

	public void setImport(boolean isImport) {
		this.isImport = isImport;
	}

}
