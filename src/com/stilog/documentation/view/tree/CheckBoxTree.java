package com.stilog.documentation.view.tree;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.stilog.analysevpi.view.tree.CustomTreeCellRenderer;
import com.stilog.vpimodel.objects.Attribute;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.VPIDatas;
import com.stilog.vpimodel.vpsettings.FileDatas;

public class CheckBoxTree extends JTree {
    
    public CheckBoxTree(CheckBoxTreeNode root) {
        super(root);
        
        // Utiliser le renderer personnalisé
        setCellRenderer(new CheckBoxTreeCellRenderer());
        
        // Ajouter le listener pour gérer les clics
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = getRowForLocation(e.getX(), e.getY());
                if (row != -1) {
                    TreePath path = getPathForRow(row);
                    Object node = path.getLastPathComponent();
                    
                    if (node instanceof CheckBoxTreeNode) {
                        CheckBoxTreeNode checkNode = (CheckBoxTreeNode) node;
                        
                        // Vérifier si le clic est sur la zone de la checkbox
                            // Toggle la sélection
                            checkNode.setSelected(!checkNode.isSelected());
                            
                            // Propager aux enfants (optionnel)
                            toggleChildren(checkNode, checkNode.isSelected());
                            
                            // Mettre à jour le parent (optionnel)
                            updateParent(checkNode);
                            
                            // Rafraîchir l'affichage
                            repaint();
                    }
                }
            }
        });
    }
    
	/*
	 * METHODS
	 */
	public void update(VPIDatas datas) {
		try {
			this.setCellRenderer(new CheckBoxTreeCellRenderer());

			DefaultMutableTreeNode root = new DefaultMutableTreeNode(datas);

			List<FileDatas> datasFiles = datas.getFilesDatas();

			for (int i = 0; i < datasFiles.size(); i++) {
				FileDatas fileDatas = datasFiles.get(i);
				if (fileDatas == null || fileDatas.getEntities().isEmpty())
					continue;
				fileDatas.sort();
				DefaultMutableTreeNode fileNode = computeFileNode(fileDatas);

				root.add(fileNode);
			}

			this.setModel(new DefaultTreeModel(root));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private DefaultMutableTreeNode computeFileNode(FileDatas datas) {

		DefaultMutableTreeNode datasNode = new DefaultMutableTreeNode(datas);

		for (Entity entity : datas.getEntities()) {
			DefaultMutableTreeNode entityNode = null;
			if(entity.isDocumentable())
				entityNode = new CheckBoxTreeNode(entity);
			else
				entityNode = new DefaultMutableTreeNode(entity);

			for (Parameters param : entity.getParameters()) {
				DefaultMutableTreeNode paramNode = null;
				if(param.isDocumentable())
					paramNode = new CheckBoxTreeNode(param);
				else
					paramNode = new DefaultMutableTreeNode(param);

				for (Attribute attr : param.getAttributes()) {
					DefaultMutableTreeNode attributeNode = new DefaultMutableTreeNode(attr);
					paramNode.add(attributeNode);
				}
				entityNode.add(paramNode);
			}
			datasNode.add(entityNode);
		}

		return datasNode;
	}
    
    /**
     * Coche/décoche tous les enfants d'un nœud
     */
    private void toggleChildren(CheckBoxTreeNode node, boolean selected) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (child instanceof CheckBoxTreeNode) {
                CheckBoxTreeNode childNode = (CheckBoxTreeNode) child;
                childNode.setSelected(selected);
                toggleChildren(childNode, selected);
            }
        }
    }
    
    /**
     * Met à jour l'état du parent en fonction des enfants
     */
    private void updateParent(DefaultMutableTreeNode node) {
    	if(node.getParent() instanceof CheckBoxTreeNode) {
	    	CheckBoxTreeNode parent = (CheckBoxTreeNode) node.getParent();
	        if (parent != null) {
	            // Vérifier si tous les enfants sont cochés
	            boolean allSelected = true;
	            boolean anySelected = false;
	            
	            for (int i = 0; i < parent.getChildCount(); i++) {
	                Object child = parent.getChildAt(i);
	                if (child instanceof CheckBoxTreeNode) {
	                    CheckBoxTreeNode childNode = (CheckBoxTreeNode) child;
	                    if (childNode.isSelected()) {
	                        anySelected = true;
	                    } else {
	                        allSelected = false;
	                    }
	                }
	            }
	            
	            // Le parent est coché seulement si tous les enfants le sont
	            parent.setSelected(anySelected);
	            
	            // Remonter récursivement
	            updateParent(parent);
	        }
    	}
    }
    
    /**
     * Récupère tous les nœuds cochés
     */
    public List<CheckBoxTreeNode> getSelectedNodes() {
        List<CheckBoxTreeNode> selected = new java.util.ArrayList<>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
        collectSelectedNodes(root, selected);
        return selected;
    }
    
    private void collectSelectedNodes(DefaultMutableTreeNode node, List<CheckBoxTreeNode> selected) {
    	if (node instanceof CheckBoxTreeNode && ((CheckBoxTreeNode)node).isSelected()) {
            selected.add((CheckBoxTreeNode) node);
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            collectSelectedNodes((DefaultMutableTreeNode) child, selected);
        }
    }
}
