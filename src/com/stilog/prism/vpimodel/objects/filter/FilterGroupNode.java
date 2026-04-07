package com.stilog.prism.vpimodel.objects.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Nœud groupe d'un arbre de filtre VP : AND ou OR d'un ensemble de FilterNode enfants.
 * Remplace FilterLogicGroup qui ne supportait qu'un seul niveau.
 */
public class FilterGroupNode extends FilterNode {

    private final String operator;           // "AND" ou "OR"
    private final List<FilterNode> children = new ArrayList<>();

    public FilterGroupNode(String operator) {
        this.operator = operator != null ? operator : "AND";
    }

    public void addChild(FilterNode child) {
        children.add(child);
    }

    public String getOperator()          { return operator; }
    public List<FilterNode> getChildren(){ return children; }
    public boolean isEmpty()             { return children.isEmpty(); }

    @Override public boolean isGroup()   { return true; }
}
