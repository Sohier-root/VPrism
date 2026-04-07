package com.stilog.prism.vpimodel.objects.filter;

/**
 * Feuille d'un arbre de filtre VP : condition atomique sur un attribut.
 * Remplace FilterConditionData.
 */
public class FilterLeafNode extends FilterNode {

    private final String attributeTitle;  // ex: "Nom Intervention"
    private final String operator;        // ex: "STARTWITHS"
    private final String valueDisplay;    // ex: "MES", "Oui", "$dateFin"
    private final boolean dynamic;        // true = valeur fournie dynamiquement

    public FilterLeafNode(String attributeTitle, String operator,
                          String valueDisplay, boolean dynamic) {
        this.attributeTitle = attributeTitle;
        this.operator      = operator;
        this.valueDisplay  = valueDisplay;
        this.dynamic       = dynamic;
    }

    public String getAttributeTitle() { return attributeTitle; }
    public String getOperator()       { return operator; }
    public String getValueDisplay()   { return valueDisplay; }
    public boolean isDynamic()        { return dynamic; }

    @Override public boolean isLeaf() { return true; }
}
