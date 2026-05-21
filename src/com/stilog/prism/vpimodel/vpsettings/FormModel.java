package com.stilog.prism.vpimodel.vpsettings;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.stilog.prism.comparevpi.utils.MethodUtil;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.TypeData;
import com.stilog.prism.vpimodel.utils.VPIConstants;

public class FormModel extends FileDatas {

    /** Types de headings considérés comme "calculés" → non-mergeables. */
    private static final java.util.Set<String> COMPUTED_TYPES = java.util.Set.of(
        "ControlColumn",
        "HeadingOperation",
        "StringHeadingOperation",
        "StringControlColumn"
    );

    public FormModel(String filePath, String name) {
        super(filePath, name);
    }

    // ── Parsing XML ──────────────────────────────────────────────────────────

    @Override
    protected List<Parameters> parseXml(Entity entity) {
        entity.setMergeable(true);
        entity.setReplaceable(true);
        entity.setTypeData(TypeData.FORM);

        List<Parameters> paramList = new ArrayList<>();

        org.w3c.dom.Document doc = getDocument(entity.getAssociatedXml());
        Element root = (Element) doc.getDocumentElement();

        // Attributs uniques
        String id  = root.getElementsByTagName(VPIConstants.XML_TAG_ID) .item(0).getTextContent();
        String uid = root.getElementsByTagName(VPIConstants.XML_TAG_UID).item(0).getTextContent();
        entity.addUniqueAttributes(VPIConstants.XML_TAG_ID,  id);
        entity.addUniqueAttributes(VPIConstants.XML_TAG_UID, uid);

        com.stilog.prism.comparevpi.model.GeneralCorrespondance gCorr =
            com.stilog.prism.comparevpi.model.GeneralCorrespondance.getInstance();
        gCorr.addCorrespondance(VPIConstants.XML_TAG_ID,  id,  entity.getName());
        gCorr.addCorrespondance(VPIConstants.XML_TAG_UID, uid, entity.getName());
        gCorr.addCorrespondance(VPIConstants.PARAMETER_FORM_MODEL, id, entity.getName());

        // Attributs cachés
        String comments = root.getElementsByTagName(VPIConstants.XML_TAG_COMMENTS).item(0).getTextContent();
        entity.addHiddenAttributes(VPIConstants.PARAMETER_COMMENTS, comments);

        // Headings
        Node headingsNode = root.getElementsByTagName(VPIConstants.XML_TAG_HEADINGS).item(0);
        if (headingsNode != null) {
            paramList.addAll(computeHeadings(headingsNode));
        }

        return paramList;
    }

    /**
     * Parse les {@code <com.visualplanning.data.Heading>} du formulaire.
     * Même logique que {@code ResourceModel.computeHeadings()}, adaptée aux
     * types spécifiques des formulaires.
     */
    private List<Parameters> computeHeadings(Node parentNode) {
        List<Parameters> headings = new ArrayList<>();
        NodeList headingsList = parentNode.getChildNodes();

        for (int i = 0; i < headingsList.getLength(); i++) {
            Node heading = headingsList.item(i);
            if (heading.getNodeType() != Node.ELEMENT_NODE) continue;

            Element h = (Element) heading;

            String id   = h.getElementsByTagName(VPIConstants.XML_TAG_ID) .item(0).getTextContent();
            String uid  = h.getElementsByTagName(VPIConstants.XML_TAG_UID).item(0).getTextContent();
            String name = h.getElementsByTagName(VPIConstants.XML_TAG_NAME).item(0).getTextContent();

            Element typeEl = (Element) h.getElementsByTagName(VPIConstants.XML_TAG_TYPE).item(0);
            String typeClass = typeEl != null ? typeEl.getAttribute("class") : "";

            // Libellé lisible du type
            String typeLabel = resolveTypeLabel(typeClass);

            Parameters param = new Parameters(name);
            param.setParentTag(parentNode.getNodeName());
            param.setUid(uid);
            param.setInitialXml(MethodUtil.nodeToString(heading));
            param.setAssociatedXml(MethodUtil.nodeToString(heading));

            // Champs calculés → lecture seule
            boolean isComputed = COMPUTED_TYPES.contains(typeClass)
                || typeClass.contains("FileType");
            param.setMergeable(!isComputed);
            param.setReplaceable(!isComputed);

            param.addUniqueAttributes(VPIConstants.XML_TAG_ID,  id);
            param.addUniqueAttributes(VPIConstants.XML_TAG_UID, uid);

            com.stilog.prism.comparevpi.model.GeneralCorrespondance gCorr =
                com.stilog.prism.comparevpi.model.GeneralCorrespondance.getInstance();
            gCorr.addCorrespondance(VPIConstants.XML_TAG_ID,  id,  name);
            gCorr.addCorrespondance(VPIConstants.XML_TAG_UID, uid, name);

            // Attributs visibles
            param.addAttributes(VPIConstants.PARAMETER_NAME, name);
            param.addAttributes(VPIConstants.PARAMETER_TYPE, typeLabel);

            // Liste à choix unique
            NodeList uniqueValues = h.getElementsByTagName(VPIConstants.XML_TAG_UNIQUE_VALUE_LIST);
            if (uniqueValues != null && uniqueValues.getLength() > 0) {
                param.addAttributes(VPIConstants.PARAMETER_VALUE_LIST,
                    extractValueList(uniqueValues.item(0)));
            }

            // Liste à choix multiple
            NodeList multiValues = h.getElementsByTagName(VPIConstants.XML_TAG_MULTI_VALUE_LIST);
            if (multiValues != null && multiValues.getLength() > 0) {
                param.addAttributes(VPIConstants.PARAMETER_VALUE_LIST,
                    extractValueList(multiValues.item(0)));
            }

            // Attributs cachés
            String comments    = h.getElementsByTagName(VPIConstants.XML_TAG_COMMENTS).item(0).getTextContent();
            Node   defaultNode = h.getElementsByTagName(VPIConstants.XML_TAG_DEFAULT_VAL).item(0);
            String defaultVal  = defaultNode != null ? defaultNode.getTextContent() : "";
            Node   lengthNode  = typeEl != null ? typeEl.getElementsByTagName(VPIConstants.XML_TAG_LENGHT).item(0) : null;
            String length      = lengthNode != null ? lengthNode.getTextContent() : "";
            Node   patternNode = h.getElementsByTagName(VPIConstants.XML_TAG_PATTERN).item(0);
            String pattern     = patternNode != null ? patternNode.getTextContent() : "";

            param.addHiddenAttributes(VPIConstants.PARAMETER_COMMENTS,     comments);
            param.addHiddenAttributes(VPIConstants.PARAMETER_DEFAULT_VAL,  defaultVal);
            param.addHiddenAttributes(VPIConstants.PARAMETER_LENGHT,       length);
            param.addHiddenAttributes(VPIConstants.PARAMETER_PATTERN,      pattern);

            headings.add(param);
        }
        return headings;
    }

    /** Convertit la classe technique VP en libellé lisible. */
    private static String resolveTypeLabel(String typeClass) {
        if (typeClass == null || typeClass.isBlank()) return "?";
        // Enlever le package si présent
        int dot = typeClass.lastIndexOf('.');
        String simple = dot >= 0 ? typeClass.substring(dot + 1) : typeClass;
        return switch (simple) {
            case "StringType"             -> "Texte";
            case "MultiLineType"          -> "Texte multiligne";
            case "DoubleType"             -> "Nombre";
            case "IntegerType"            -> "Entier";
            case "BooleanType"            -> "Booléen";
            case "DateTimeType"           -> "Date/Heure";
            case "UniqueChoiceType"       -> "Liste (unique)";
            case "MultipleChoiceType"     -> "Liste (multiple)";
            case "ResourceReference"      -> "Référence dimension";
            case "StringHeadingOperation" -> "Calculé (texte)";
            case "HeadingOperation"       -> "Calculé (nombre)";
            case "ControlColumn"          -> "Calculé (contrôle)";
            case "StringControlColumn"    -> "Calculé (ctrl texte)";
            case "FileType"               -> "Fichier/Image";
            default                       -> simple;
        };
    }

    /** Extrait la liste de valeurs d'un nœud valueList. */
    private static String extractValueList(Node listNode) {
        StringBuilder sb = new StringBuilder();
        NodeList items = listNode.getChildNodes();
        for (int j = 0; j < items.getLength(); j++) {
            if (items.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
            Element item = (Element) items.item(j);
            NodeList vals = item.getElementsByTagName(VPIConstants.XML_TAG_VALUE);
            if (vals.getLength() > 0) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(vals.item(0).getTextContent());
            }
        }
        return sb.toString();
    }
}
