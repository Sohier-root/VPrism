package com.stilog.analysevpi.view.tree;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.stilog.analysevpi.model.objects.Attribute;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.model.objects.VPIDatas;
import com.stilog.analysevpi.model.vpsettings.FileDatas;

public class CustomTreeCellRenderer extends DefaultTreeCellRenderer{

	private static final long serialVersionUID = 1L;

	@Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {

        JLabel c = (JLabel) super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);
        
		/*
		 * COULEUR
		 */

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object obj = node.getUserObject();

        if (obj instanceof Attribute fd) {
        	c.setText(fd.toString());
            if (fd.isAnomaly()) {
                c.setForeground(Color.RED);
            } else {
                c.setForeground(Color.BLACK);
            }
        }
        
        if (obj instanceof Parameters fd) {
        	c.setText(fd.getName());
            if (fd.isAnomaly()) {
                c.setForeground(Color.RED);
            } else {
                c.setForeground(Color.BLACK);
            }
        }
        
        if (obj instanceof Entity fd) {
        	c.setText(fd.getName());
            if (fd.isAnomaly()) {
                c.setForeground(Color.RED);
            } else {
                c.setForeground(Color.BLACK);
            }
        }
        
        if (obj instanceof FileDatas fd) {
        	c.setText(fd.getName());
            if (fd.isAnomaly()) {
                c.setForeground(Color.RED);
            } else {
                c.setForeground(Color.BLACK);
            }
        }
        
        if (obj instanceof VPIDatas fd) {
        	c.setText(fd.getName());
        }

        return c;
    }
}
