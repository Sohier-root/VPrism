package com.stilog.analysevpi.view.tree;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.stilog.vpimodel.objects.Attribute;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.VPIDatas;
import com.stilog.vpimodel.vpsettings.FileDatas;

public class CustomTreeCellRenderer extends DefaultTreeCellRenderer{

	private static final long serialVersionUID = 1L;

	private static final float[] RESOLVE_HSB = Color.RGBtoHSB(1, 150, 0, null);
	private static final float[] ANOMALY_HSB = Color.RGBtoHSB(237, 27, 25, null);
	
	private static final Color RESOLVE_COLOR = Color.getHSBColor(RESOLVE_HSB[0], RESOLVE_HSB[1], RESOLVE_HSB[2]);
	private static final Color ANOMALY_COLOR = Color.getHSBColor(ANOMALY_HSB[0], ANOMALY_HSB[1], ANOMALY_HSB[2]);
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
            	if(fd.isResolve())
                	c.setForeground(RESOLVE_COLOR);
            	else
            		c.setForeground(ANOMALY_COLOR);
            } else {
                c.setForeground(UIManager.getColor("Tree.foreground"));
            }
        }
        
        if (obj instanceof Parameters fd) {
        	c.setText(fd.getName());
            if (fd.isAnomaly()) {
            	if(fd.isResolve()) {
                	c.setForeground(RESOLVE_COLOR);
            	}
            	else
            		c.setForeground(ANOMALY_COLOR);
            }else {
                c.setForeground(Color.BLACK);
            }
        }
        
        if (obj instanceof Entity fd) {
        	c.setText(fd.getName());
            if (fd.isAnomaly()) {
            	if(fd.isResolve()) {
                	c.setForeground(RESOLVE_COLOR);
            	}
            	else
            		c.setForeground(ANOMALY_COLOR);
            } else {
                c.setForeground(Color.BLACK);
            }
        }
        
        if (obj instanceof FileDatas fd) {
        	c.setText(fd.getName());
            if (fd.isAnomaly()) {
                c.setForeground(ANOMALY_COLOR);
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
