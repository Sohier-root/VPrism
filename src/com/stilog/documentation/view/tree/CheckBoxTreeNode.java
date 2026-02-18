package com.stilog.documentation.view.tree;

import javax.swing.tree.DefaultMutableTreeNode;

public class CheckBoxTreeNode extends DefaultMutableTreeNode {
    private boolean selected;
    private boolean enabled = true;
    
    public CheckBoxTreeNode(Object userObject) {
        super(userObject);
        this.selected = false;
    }
    
    public CheckBoxTreeNode(Object userObject, boolean selected) {
        super(userObject);
        this.selected = selected;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
