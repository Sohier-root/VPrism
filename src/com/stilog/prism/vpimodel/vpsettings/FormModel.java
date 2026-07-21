package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;

import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.TypeData;
import com.stilog.prism.vpimodel.utils.VPIConstants;

public class FormModel extends FileDatas {

    public FormModel(String filePath, String name) {
        super(filePath, name);
    }

    // ── Parsing (via VPIReader) ─────────────────────────────────────────────

    @Override
    protected List<Parameters> buildParameters(Entity entity) {
        entity.setTypeData(TypeData.FORM);

        com.visualplanning.vpi.model.form.FormModel form = findForm(entity.getId());
        if (form == null) {
            System.out.println("Formulaire introuvable pour l'entité id=" + entity.getId());
            return new ArrayList<>();
        }

        entity.addHiddenAttributes(VPIConstants.PARAMETER_COMMENTS, form.getDescription().getDisplayValue());

        return computeHeadingParameters(form.getHeadings());
    }

    private com.visualplanning.vpi.model.form.FormModel findForm(int id) {
        for (com.visualplanning.vpi.model.form.FormModel f : this.planning.getForms()) {
            if (f.getId() == id)
                return f;
        }
        return null;
    }
}
