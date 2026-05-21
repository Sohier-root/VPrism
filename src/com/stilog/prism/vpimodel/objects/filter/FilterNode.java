package com.stilog.prism.vpimodel.objects.filter;

/**
 * Nœud de l'arbre de conditions d'un filtre VP.
 * Peut être soit un groupe logique (FilterGroupNode), soit une condition atomique (FilterLeafNode).
 */
public abstract class FilterNode {
    // Marqueur de type pour éviter instanceof répété
    public boolean isGroup() { return false; }
    public boolean isLeaf()  { return false; }
}
