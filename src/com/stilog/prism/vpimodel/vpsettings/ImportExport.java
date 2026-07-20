package com.stilog.prism.vpimodel.vpsettings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.stilog.prism.comparevpi.utils.MethodUtil;
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

	/**
	 * XML brut par entité (id → XML décodé), pour {@code readKeyTitles()} uniquement :
	 * VPIReader ne couvre pas encore keyAttributes/parentKeyAttributes (cf. {@link Definition}).
	 * Seule classe du modèle à encore lire du XML, en local, sans passer par le reader.
	 */
	private final Map<Integer, String> rawXmlById = new HashMap<>();

	public ImportExport(String filePath, String name) {
		super(filePath, name);
	}

	public void setContexts(List<? extends ImportExportContextBase> contexts) {
		this.contexts = contexts;
	}

	@Override
	public void parseDatas() {
		preReadRawXml();
		super.parseDatas();
	}

	private void preReadRawXml() {
		try (BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(";");
				if (values.length < 4) continue;
				int id = Integer.parseInt(values[0]);
				String xml = MethodUtil.decodeBase64(values[3].replace("\"", ""));
				rawXmlById.put(id, xml);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * PARSE (via VPIReader, sauf Cle/Cle parent : non couvert par VPIReader,
	 * conservé en lecture DOM directe sur le fragment XML de l'entité)
	 */
	@Override
	protected List<Parameters> buildParameters(Entity entity) {

		List<Parameters> paramList = new ArrayList<>();
		ImportExportContextBase context = findContext(entity.getId());
		if (context == null) {
			System.out.println("Contexte import/export introuvable pour l'entité id=" + entity.getId());
			return paramList;
		}

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
		paramSrc.addAttributes(VPIConstants.PARAMETER_FORMAT, src.format());
		paramSrc.addAttributes(VPIConstants.PARAMETER_ENCODING, src.encoding());
		paramSrc.addAttributes(VPIConstants.PARAMETER_SEPARATOR, src.separator());
		paramList.add(paramSrc);

		/*
		 * CLE / CLE PARENT — non couvert par VPIReader (Definition n'expose pas
		 * keyAttributes/parentKeyAttributes) : lu directement dans le fragment XML brut.
		 */
		String entityXml = rawXmlById.get(entity.getId());
		List<String> keyTitles = readKeyTitles(entityXml, VPIConstants.XML_TAG_KEY_ATTRIBUTES);
		if (!keyTitles.isEmpty()) {
			Parameters paramKey = new Parameters(VPIConstants.PARAMETER_KEY);
			for (int i = 0; i < keyTitles.size(); i++)
				paramKey.addAttributes(VPIConstants.PARAMETER_KEY + i, keyTitles.get(i));
			paramList.add(paramKey);
		}

		List<String> keyParentTitles = readKeyTitles(entityXml, VPIConstants.XML_TAG_PARENT_KEY_ATTRIBUTES);
		if (!keyParentTitles.isEmpty()) {
			Parameters paramKeyParent = new Parameters(VPIConstants.PARAMETER_KEY_PARENT);
			for (int i = 0; i < keyParentTitles.size(); i++)
				paramKeyParent.addAttributes(VPIConstants.PARAMETER_KEY_PARENT + i, keyParentTitles.get(i));
			paramList.add(paramKeyParent);
		}

		/*
		 * PARAMETRE
		 */
		Parameters paramParam = new Parameters(VPIConstants.PARAMETER_PARAMETRE);

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
		if (entityXml == null)
			return titles;
		try {
			Document doc = parseXmlFragment(entityXml);
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

	private Document parseXmlFragment(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
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
