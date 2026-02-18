package com.stilog.documentation.view.tree;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import com.stilog.vpimodel.objects.Attribute;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.VPIDatas;
import com.stilog.vpimodel.vpsettings.FileDatas;

public class CheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
    private JCheckBox checkBox;
    private DefaultTreeCellRenderer defaultRenderer;
    
    public CheckBoxTreeCellRenderer() {
        setLayout(new BorderLayout());
        checkBox = new JCheckBox();
        defaultRenderer = new DefaultTreeCellRenderer();
        
        add(checkBox, BorderLayout.WEST);
        setOpaque(false);
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
        // Déterminer le texte à afficher en fonction du type d'objet
        String displayText = getDisplayText(value);
        
        // Obtenir le rendu par défaut pour l'icône et le texte
        Component returnValue = defaultRenderer.getTreeCellRendererComponent(
                tree, displayText, selected, expanded, leaf, row, hasFocus);
        
        if (value instanceof CheckBoxTreeNode) {
            CheckBoxTreeNode node = (CheckBoxTreeNode) value;
            checkBox.setSelected(node.isSelected());
            checkBox.setEnabled(node.isEnabled());
            
            // Enlever le composant précédent s'il existe
            if (getComponentCount() > 1) {
                remove(1);
            }
            add(returnValue, BorderLayout.CENTER);
            
            // Important: rendre le panel transparent
            setOpaque(false);
            
            return this;
        }
        
        return returnValue;
    }
    
    /**
     * Extrait le texte à afficher en fonction du type de nœud
     */
    private String getDisplayText(Object value) {
        if (!(value instanceof DefaultMutableTreeNode)) {
            return value.toString();
        }
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object obj = node.getUserObject();
        
        if (obj instanceof Attribute) {
            Attribute attr = (Attribute) obj;
            return attr.toString();
        }
        
        if (obj instanceof Parameters) {
            Parameters param = (Parameters) obj;
            return param.getName();
        }
        
        if (obj instanceof Entity) {
            Entity entity = (Entity) obj;
            return entity.getName();
        }
        
        if (obj instanceof FileDatas) {
            FileDatas fd = (FileDatas) obj;
            return fd.getName();
        }
        
        if (obj instanceof VPIDatas) {
            VPIDatas vpi = (VPIDatas) obj;
            return vpi.getName();
        }
        
        return obj != null ? obj.toString() : "";
    }
}
