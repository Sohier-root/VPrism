package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stilog.prism.comparevpi.model.GeneralCorrespondance;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.TypeData;
import com.stilog.prism.vpimodel.reader.PropertyLabels;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.visualplanning.vpi.model.dimension.Heading;
import com.visualplanning.vpi.model.dimension.heading.HeadingMultiChoice;
import com.visualplanning.vpi.model.dimension.heading.HeadingResourceReference;
import com.visualplanning.vpi.model.dimension.heading.HeadingUniqueChoice;
import com.visualplanning.vpi.model.property.Property;

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

        GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
        gCorr.addCorrespondance(VPIConstants.XML_TAG_ID, String.valueOf(form.getId()), entity.getName());
        gCorr.addCorrespondance(VPIConstants.XML_TAG_UID, form.getUid(), entity.getName());
        gCorr.addCorrespondance(VPIConstants.PARAMETER_FORM_MODEL, String.valueOf(form.getId()), entity.getName());

        entity.addHiddenAttributes(VPIConstants.PARAMETER_COMMENTS, form.getDescription().getDisplayValue());

        return computeHeadings(form);
    }

    private com.visualplanning.vpi.model.form.FormModel findForm(int id) {
        for (com.visualplanning.vpi.model.form.FormModel f : this.planning.getForms()) {
            if (f.getId() == id)
                return f;
        }
        return null;
    }

    /** Même logique que {@code ResourceModel.computeHeadings()}, adaptée aux formulaires. */
    private List<Parameters> computeHeadings(com.visualplanning.vpi.model.form.FormModel form) {
        List<Parameters> headings = new ArrayList<>();

        for (Heading heading : form.getHeadings()) {
            Parameters param = new Parameters(heading.getName());
            param.setUid(heading.getUid());

            GeneralCorrespondance gCorr = GeneralCorrespondance.getInstance();
            gCorr.addCorrespondance(VPIConstants.XML_TAG_ID, String.valueOf(heading.getId()), heading.getName());
            gCorr.addCorrespondance(VPIConstants.XML_TAG_UID, heading.getUid(), heading.getName());

            param.addAttributes(VPIConstants.PARAMETER_NAME, heading.getName());
            param.addAttributes(VPIConstants.PARAMETER_TYPE, PropertyLabels.label(heading.getHeadingType()));

            Set<Property> alreadyShown = new HashSet<>();
            if (heading instanceof HeadingResourceReference ref) {
                alreadyShown.add(ref.getReferencedDimension());
                if (ref.getReferencedDimension().getEntityId() != -1)
                    param.addAttributes(VPIConstants.PARAMETER_RESOURCEMODEL, ref.getReferencedDimension().getDisplayValue());
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

            headings.add(param);
        }
        return headings;
    }
}
