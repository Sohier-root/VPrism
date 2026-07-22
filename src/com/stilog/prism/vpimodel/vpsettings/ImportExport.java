package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;

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
	 * PARSE (via VPIReader)
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
		paramList.addAll(computeConfiguration(context, def));

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

	private List<Parameters> computeConfiguration(ImportExportContextBase context, Definition def) {
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
		 * CLE / CLE PARENT
		 */
		List<String> keyTitles = def.getKeyAttributeTitles();
		if (!keyTitles.isEmpty()) {
			Parameters paramKey = new Parameters(VPIConstants.PARAMETER_KEY);
			for (int i = 0; i < keyTitles.size(); i++)
				paramKey.addAttributes(VPIConstants.PARAMETER_KEY + i, keyTitles.get(i));
			paramList.add(paramKey);
		}

		List<String> keyParentTitles = def.getParentKeyAttributeTitles();
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
