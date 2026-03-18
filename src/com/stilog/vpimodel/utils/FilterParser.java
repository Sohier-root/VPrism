package com.stilog.vpimodel.utils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.vpimodel.objects.filter.FilterLeafNode;
import com.stilog.vpimodel.objects.filter.FilterNode;

/**
 * Parse récursivement le XML d'un nœud filterCondition VP en arbre FilterNode.
 *
 * Résout automatiquement :
 *  - INFILTER entityID=0 → "Tous" ; entityID>0 → nom du filtre via GeneralCorrespondance
 *  - Listes d'entiers sur une ResourceAttribute → "N référence(s) de ressource"
 *  - Listes d'entiers sur un attribut classique  → valeurs brutes "1, 2, 3"
 *  - isNull + variableName → "$variableName"
 */
public class FilterParser {

    private static final String TAG_CONDITIONS    = "conditions";
    private static final String TAG_OPERATOR      = "operator";
    private static final String TAG_ATTRIBUTE     = "attribute";
    private static final String TAG_TITLE         = "title";
    private static final String TAG_VALUE         = "value";
    private static final String TAG_DYNAMIC       = "dynamic";
    private static final String TAG_INTEGER       = "Integer";
    private static final String TAG_VARIABLE_NAME = "variableName";
    private static final String TAG_RESOURCE_MODEL_ID = "resourceModelID";

    private static final String SUFFIX_LOGIC  = "FilterLogicCondition";
    private static final String CLASS_LIST    = "List";
    private static final String CLASS_BOOL    = "java.lang.Boolean";
    private static final String CLASS_ENTITY_REF = "EntityReference";
    private static final String CLASS_EVENT_STRUCT = "com.visualplanning.data.filter.EventStructNodeDefinition";
    private static final String TAG_EVENT_TREE_STRUCT = "eventTreeStruct";
    private static final String TAG_ENTITY_ID = "entityID";
    private static final String OP_INFILTER   = "INFILTER";
    private static final String OP_NOTINFILTER   = "NOTINFILTER";

    
    private static final DocumentBuilderFactory DB_FACTORY = DocumentBuilderFactory.newInstance();
    /**
     * Parse le XML du nœud filterCondition et retourne la racine de l'arbre.
     *
     * @param filterConditionXml  XML du nœud filterCondition (déjà décodé)
     * @param isEventFilter       true = filtre événement, false = filtre ressource
     *                            (pour choisir la bonne table de correspondance INFILTER)
     */
    public static FilterGroupNode parse(String filterConditionXml, boolean isEventFilter) {
        try {
            Document doc = buildDocument(filterConditionXml);
            Element root = doc.getDocumentElement();
            return parseGroupNode(root, isEventFilter);
        } catch (Exception e) {
            e.printStackTrace();
            return new FilterGroupNode("?");
        }
    }

    // -----------------------------------------------------------------------
    // Parsing récursif
    // -----------------------------------------------------------------------

    private static FilterGroupNode parseGroupNode(Element groupElement, boolean isEventFilter) {
        // Opérateur logique : <operator> enfant DIRECT
        String logicOp = "AND";
        NodeList children = groupElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && TAG_OPERATOR.equals(child.getNodeName())) {
                logicOp = child.getTextContent().trim();
                break;
            }
        }

        FilterGroupNode group = new FilterGroupNode(logicOp);

        // Nœud <conditions> enfant direct
        Node conditionsNode = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && TAG_CONDITIONS.equals(child.getNodeName())) {
                conditionsNode = child;
                break;
            }
        }
        if (conditionsNode == null) return group;

        NodeList conditionNodes = conditionsNode.getChildNodes();
        for (int i = 0; i < conditionNodes.getLength(); i++) {
            Node condNode = conditionNodes.item(i);
            if (condNode.getNodeType() != Node.ELEMENT_NODE) continue;
            FilterNode child = parseNode((Element) condNode, isEventFilter);
            if (child != null) group.addChild(child);
        }
        return group;
    }

    private static FilterNode parseNode(Element element, boolean isEventFilter) {
        if (element.getNodeName().endsWith(SUFFIX_LOGIC)) {
            return parseGroupNode(element, isEventFilter);
        }
        return parseLeafNode(element, isEventFilter);
    }

    private static FilterLeafNode parseLeafNode(Element condElement, boolean isEventFilter) {
        try {
            NodeList condChildren = condElement.getChildNodes();

            // Titre de l'attribut : <attribute><title>
            String title = "";
            boolean isResourceAttribute = false;
            for (int i = 0; i < condChildren.getLength(); i++) {
                Node child = condChildren.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && TAG_ATTRIBUTE.equals(child.getNodeName())) {
                    Element attrEl = (Element) child;
                    NodeList attrChildren = attrEl.getChildNodes();
                    for (int j = 0; j < attrChildren.getLength(); j++) {
                        Node ac = attrChildren.item(j);
                        if (ac.getNodeType() == Node.ELEMENT_NODE
                                && TAG_TITLE.equals(ac.getNodeName())) {
                            title = ac.getTextContent().trim();
                        }
                        // La présence de resourceModelID indique une ResourceAttribute
                        if (ac.getNodeType() == Node.ELEMENT_NODE
                                && TAG_RESOURCE_MODEL_ID.equals(ac.getNodeName())) {
                            isResourceAttribute = true;
                        }
                    }
                    break;
                }
            }

            // Opérateur
            String operator = "";
            for (int i = 0; i < condChildren.getLength(); i++) {
                Node child = condChildren.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && TAG_OPERATOR.equals(child.getNodeName())) {
                    operator = child.getTextContent().trim();
                    break;
                }
            }

            // Dynamic
            boolean dynamic = false;
            for (int i = 0; i < condChildren.getLength(); i++) {
                Node child = condChildren.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && TAG_DYNAMIC.equals(child.getNodeName())) {
                    dynamic = Boolean.parseBoolean(child.getTextContent().trim());
                    break;
                }
            }

            // Variable name
            String variableName = "";
            for (int i = 0; i < condChildren.getLength(); i++) {
                Node child = condChildren.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && TAG_VARIABLE_NAME.equals(child.getNodeName())) {
                    variableName = child.getTextContent().trim();
                    break;
                }
            }

            String valueDisplay = buildValueDisplay(
                    condElement, operator, dynamic, variableName,
                    isResourceAttribute, isEventFilter);

            return new FilterLeafNode(title, operator, valueDisplay, dynamic);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Construction de la valeur affichable
    // -----------------------------------------------------------------------

    private static String buildValueDisplay(Element condElement,
                                            String operator,
                                            boolean dynamic,
                                            String variableName,
                                            boolean isResourceAttribute,
                                            boolean isEventFilter) {
        // Chercher <value> enfant direct
        Node valueNode = null;
        NodeList children = condElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && TAG_VALUE.equals(child.getNodeName())) {
                valueNode = child;
                break;
            }
        }
        if (valueNode == null) return "";

        Element valueEl = (Element) valueNode;

        // isNull="true" → variable dynamique ou null
        if ("true".equalsIgnoreCase(valueEl.getAttribute("isNull"))) {
            if (!variableName.isEmpty()) return "$" + variableName;
            return dynamic ? "(valeur dynamique)" : "(null)";
        }

        String clazz = valueEl.getAttribute("class");

        // --- ISA : référence à une hiérarchie d'événement (EventTreeStruct) ---
        if (CLASS_EVENT_STRUCT.equals(clazz)) {
            NodeList etsNodes = valueEl.getElementsByTagName(TAG_EVENT_TREE_STRUCT);
            if (etsNodes.getLength() > 0) {
                NodeList eidNodes = ((Element) etsNodes.item(0)).getElementsByTagName(TAG_ENTITY_ID);
                if (eidNodes.getLength() > 0) {
                    String entityId = eidNodes.item(0).getTextContent().trim();
                    String name = GeneralCorrespondance.getInstance()
                            .getCorrespondance(VPIConstants.XML_TAG_TREESTRUCT, entityId);
                    if (name != null && !name.isBlank()) return name;
                    return "Hiérarchie #" + entityId;
                }
            }
        }

        // --- INFILTER : référence à un autre filtre ---
        if ((OP_INFILTER.equals(operator) || OP_NOTINFILTER.equals(operator)) && CLASS_ENTITY_REF.equals(clazz)) {
            String entityId = "";
            NodeList eidNodes = valueEl.getElementsByTagName("entityID");
            if (eidNodes.getLength() > 0) {
                entityId = eidNodes.item(0).getTextContent().trim();
            }
            return resolveFilterReference(entityId, isEventFilter);
        }

        // --- Liste d'entiers ---
        if (CLASS_LIST.equals(clazz)) {
            NodeList integers = valueEl.getElementsByTagName(TAG_INTEGER);
            int count = integers.getLength();
            if (count == 0) return "";

            if (isResourceAttribute) {
                // Les entiers sont des IDs de ressources → affichage générique
                return count == 1
                        ? "1 ressource référencée"
                        : count + " ressources référencées";
            } else {
                // Vraies valeurs de liste à choix → affichage brut
                List<String> vals = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    vals.add(integers.item(i).getTextContent().trim());
                }
                return String.join(", ", vals);
            }
        }

        // --- Booléen ---
        if (CLASS_BOOL.equals(clazz)) {
            return "true".equals(valueEl.getTextContent().trim()) ? "Oui" : "Non";
        }

        // --- Valeur brute (String, etc.) ---
        String raw = valueEl.getTextContent().trim();
        if (raw.isEmpty()) {
            if (!variableName.isEmpty()) return "$" + variableName;
            return dynamic ? "(valeur dynamique)" : "";
        }
        return raw;
    }

    /**
     * Résout une référence de filtre (INFILTER) via GeneralCorrespondance.
     * entityID=0 → "Tous" (filtre courant de la vue).
     * entityID>0 → cherche dans la table de correspondance appropriée.
     */
    private static String resolveFilterReference(String entityId, boolean isEventFilter) {
        if ("0".equals(entityId) || entityId.isEmpty()) {
            return "Tous";
        }
        String key = isEventFilter
                ? VPIConstants.XML_TAG_FILTER_EVENT
                : VPIConstants.XML_TAG_FILTER_RESOURCE;
        String name = GeneralCorrespondance.getInstance().getCorrespondance(key, entityId);
        if (name == null || name.isBlank()) {
            return "Filtre #" + entityId;
        }
        return name;
    }

    // -----------------------------------------------------------------------
    // Utilitaire DOM
    // -----------------------------------------------------------------------

    private static Document buildDocument(String xml) throws Exception {
        DB_FACTORY.setNamespaceAware(false);
        DocumentBuilder builder = DB_FACTORY.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }
}
