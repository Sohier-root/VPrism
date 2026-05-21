package com.stilog.prism.comparevpi.view.tree;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.stilog.prism.vpimodel.objects.Attribute;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.Resolveable;
import com.stilog.prism.vpimodel.objects.VPIDatas;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;

public class CustomTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	private static final Color RESOLVE_COLOR = Color.getHSBColor(
		Color.RGBtoHSB(1, 150, 0, null)[0],
		Color.RGBtoHSB(1, 150, 0, null)[1],
		Color.RGBtoHSB(1, 150, 0, null)[2]);

	private static final Color ANOMALY_COLOR = Color.getHSBColor(
		Color.RGBtoHSB(237, 27, 25, null)[0],
		Color.RGBtoHSB(237, 27, 25, null)[1],
		Color.RGBtoHSB(237, 27, 25, null)[2]);

	private static final Color CHANGED_COLOR = Color.getHSBColor(
		Color.RGBtoHSB(210, 120, 0, null)[0],
		Color.RGBtoHSB(210, 120, 0, null)[1],
		Color.RGBtoHSB(210, 120, 0, null)[2]);

	@Override
	public Component getTreeCellRendererComponent(
			JTree tree, Object value, boolean selected,
			boolean expanded, boolean leaf, int row, boolean hasFocus) {

		JLabel c = (JLabel) super.getTreeCellRendererComponent(
				tree, value, selected, expanded, leaf, row, hasFocus);

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object obj = node.getUserObject();

		String label = null;
		Resolveable r = null;

		if (obj instanceof Attribute fd)   { label = fd.toString();  r = fd; }
		else if (obj instanceof Parameters fd) { label = fd.getName();   r = fd; }
		else if (obj instanceof Entity fd)     { label = fd.getName();   r = fd; }
		else if (obj instanceof FileDatas fd)  { label = fd.getName();   r = fd; }
		else if (obj instanceof VPIDatas fd)   { label = fd.getName(); }

		if (label != null) c.setText(label);

		if (r != null) {
			c.setForeground(resolveColor(r));
		}

		return c;
	}

	/**
	 * Priorité des couleurs :
	 * 1. resolve (vert)  — résolu, prime sur tout
	 * 2. anomaly (rouge) — absent
	 * 3. changed (orange)— valeur différente
	 * 4. neutre          — aucun problème
	 */
	private Color resolveColor(Resolveable r) {
		if (r.isResolve())  return RESOLVE_COLOR;
		if (r.isAnomaly())  return ANOMALY_COLOR;
		if (r.isChanged())  return CHANGED_COLOR;
		return UIManager.getColor("Tree.foreground");
	}
}
