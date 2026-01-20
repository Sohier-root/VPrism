package com.stilog.analysevpi.view.tree;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.stilog.analysevpi.controller.Controller;
import com.stilog.analysevpi.model.objects.Attribute;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.model.objects.VPIDatas;
import com.stilog.analysevpi.model.vpsettings.ResourceModel;
import com.stilog.analysevpi.model.vpsettings.FileDatas;
import com.stilog.analysevpi.model.vpsettings.Filter;

public class VPITree extends JTree{

	private static final long serialVersionUID = 1L;
	
	private Controller controller;
	private VPITree otherTree;
	
	boolean isRefTree = false;
	boolean onlyDiff = false;
	
	public VPITree(Controller controller, boolean isRefTree) {
		super();
		this.isRefTree = isRefTree;
		this.controller = controller;
		
		DefaultMutableTreeNode VPIDatasNode = new DefaultMutableTreeNode("VPI Datas");
		this.setModel(new DefaultTreeModel(VPIDatasNode));
		
		JTree tree = this;
		this.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mousePressed(MouseEvent e) {
		    }

		    @Override
		    public void mouseReleased(MouseEvent e) {
		        if (e.isPopupTrigger()) {
		            showPopup(e);
		        }
		        else {
		        	if(!e.isControlDown())
		        		otherTree.clearSelection();
		        }
		    }

		    private void showPopup(MouseEvent e) {
		        int row = tree.getRowForLocation(e.getX(), e.getY());
		        TreePath path = tree.getPathForLocation(e.getX(), e.getY());

		        if (path == null) return;

		        tree.setSelectionPath(path);  // sélection du nœud cliqué

		        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

		        // Vérifie si ce nœud supporte le menu
		        if (nodeAllowsContextMenu(node)) {
		            JPopupMenu menu = createPopupMenuForNode(node);
		            menu.show(tree, e.getX(), e.getY());
		        }
		    }
		});
	}
	
	public void setOtherTree(VPITree otherTree) {
		this.otherTree = otherTree;
	}


	/*
	 * METHODS
	 */
	public void update(VPIDatas datas, boolean onlyDiff) {
	    try {
	        this.onlyDiff = onlyDiff;
	        this.setCellRenderer(new CustomTreeCellRenderer());
	        
	        // Sauvegarder l'état d'expansion avant la mise à jour
	        Set<TreePath> expandedPaths = new HashSet<>();
	        saveExpandedPaths(expandedPaths);
	        
	        DefaultMutableTreeNode root = new DefaultMutableTreeNode(datas);
	        
	        List<Field> datasFiles = datas.getFilesDatas();
	        
	        for(int i = 0; i<datasFiles.size(); i++) {
	            FileDatas fileDatas = (FileDatas) datasFiles.get(i).get(datas);
	            if(fileDatas == null)
	                continue;
	            fileDatas.sort();
	            DefaultMutableTreeNode fileNode = computeFileNode(fileDatas);
	            if(onlyDiff && fileNode.getChildCount() == 0)
	                continue;
	            
	            root.add(fileNode);
	        }
	        
	        this.setModel(new DefaultTreeModel(root));
	        
	        // Restaurer l'état d'expansion après la mise à jour
	        restoreExpandedPaths(expandedPaths);
	    }
	    catch(Exception e) {
	        e.printStackTrace();
	    }
	}

	/**
	 * Sauvegarde tous les chemins actuellement expansés dans l'arbre
	 */
	private void saveExpandedPaths(Set<TreePath> expandedPaths) {
	    TreeNode root = (TreeNode) this.getModel().getRoot();
	    if (root == null) return;
	    
	    // Parcourir récursivement tous les nœuds
	    saveExpandedPathsRecursive(new TreePath(root), expandedPaths);
	}

	/**
	 * Méthode récursive pour sauvegarder les chemins expansés
	 */
	private void saveExpandedPathsRecursive(TreePath parent, Set<TreePath> expandedPaths) {
	    TreeNode node = (TreeNode) parent.getLastPathComponent();
	    
	    // Si le nœud est expansé, on sauvegarde son chemin
	    if (this.isExpanded(parent)) {
	        expandedPaths.add(parent);
	    }
	    
	    // Parcourir les enfants
	    for (int i = 0; i < node.getChildCount(); i++) {
	        TreeNode child = node.getChildAt(i);
	        saveExpandedPathsRecursive(parent.pathByAddingChild(child), expandedPaths);
	    }
	}

	/**
	 * Restaure l'état d'expansion des nœuds après une mise à jour
	 */
	private void restoreExpandedPaths(Set<TreePath> expandedPaths) {
	    for (TreePath oldPath : expandedPaths) {
	        // Convertir l'ancien chemin vers le nouveau modèle
	        TreePath newPath = findEquivalentPath(oldPath);
	        if (newPath != null) {
	            this.expandPath(newPath);
	        }
	    }
	}

	/**
	 * Trouve le chemin équivalent dans le nouveau modèle d'arbre
	 * en comparant les toString() des nœuds
	 */
	private TreePath findEquivalentPath(TreePath oldPath) {
	    Object[] oldNodes = oldPath.getPath();
	    TreeNode current = (TreeNode) this.getModel().getRoot();
	    
	    // Construire le nouveau chemin étape par étape
	    Object[] newNodes = new Object[oldNodes.length];
	    newNodes[0] = current;
	    
	    // Commencer à 1 pour ignorer la racine (déjà connue)
	    for (int i = 1; i < oldNodes.length; i++) {
	        TreeNode match = findMatchingChild(current, oldNodes[i]);
	        if (match == null) {
	            return null; // Chemin introuvable dans le nouveau modèle
	        }
	        newNodes[i] = match;
	        current = match;
	    }
	    
	    return new TreePath(newNodes);
	}
	
	private DefaultMutableTreeNode computeFileNode(FileDatas datas) {
		
		DefaultMutableTreeNode datasNode = new DefaultMutableTreeNode(datas);
		
		for(Entity entity : datas.getEntities()) {
			DefaultMutableTreeNode entityNode = new DefaultMutableTreeNode(entity);
			
			if(this.onlyDiff && !entity.isAnomaly())
				continue;

			for(Parameters param : entity.getParameters()) {
				DefaultMutableTreeNode paramNode = new DefaultMutableTreeNode(param);
				
				if(this.onlyDiff && !param.isAnomaly())
					continue;
				
				for(Attribute attr : param.getAttributes()) {
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
	 * Noeud autorisé a afficher le menu contextuel
	 * @param node
	 * @return
	 */
	private boolean nodeAllowsContextMenu(DefaultMutableTreeNode node) {
	    return node.getUserObject() instanceof Entity || 
	    		node.getUserObject() instanceof VPIDatas ||
	    		node.getUserObject() instanceof FileDatas ||
	    		node.getUserObject() instanceof Parameters;
	}
	
	/**
	 * Création du menu contextuel
	 * @param node
	 * @return
	 */
	private JPopupMenu createPopupMenuForNode(DefaultMutableTreeNode node) {

	    Object obj = node.getUserObject();

	    JPopupMenu menu = new JPopupMenu();

	    JMenuItem infoItem = new JMenuItem("Afficher info");
	    infoItem.addActionListener(e -> showInfo(obj));
	    menu.add(infoItem);
	    
	    if((node.getUserObject() instanceof Parameters || node.getUserObject() instanceof Entity) && isRefTree) {
	    	JMenuItem mergeItem = new JMenuItem("Merge");
	    	if(node.getUserObject() instanceof Parameters) {
		    	DefaultMutableTreeNode entityNode = (DefaultMutableTreeNode) node.getParent();
		    	DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) entityNode.getParent();
		    	mergeItem.addActionListener(e -> {
		    		
		    	VPIDatas updatedData = controller.mergeParameter((FileDatas)fileDatasNode.getUserObject(), (Entity)entityNode.getUserObject(), (Parameters)node.getUserObject(), null);
		    	
		    	// Mise à jour de l'arbre de droite
	            if(otherTree != null) {
	                otherTree.update(updatedData, this.onlyDiff);
	            }
		    	});
	    	}
	    	else {
	    		DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) node.getParent();
		    	mergeItem.addActionListener(e -> {
		    		
		    	VPIDatas updatedData = controller.mergeEntity((FileDatas)fileDatasNode.getUserObject(), (Entity)node.getUserObject(), null);
		    	
		    	// Mise à jour de l'arbre de droite
	            if(otherTree != null) {
	                otherTree.update(updatedData, this.onlyDiff);
	            }
		    	});
	    	}
            
	    	menu.add(mergeItem);
	    	
	    	if(!this.isSelectionEmpty() && !otherTree.isSelectionEmpty()) {
	    		JMenuItem replaceItem = new JMenuItem("Replace");
	    		
	    		TreePath path = otherTree.getSelectionPath();
	    		DefaultMutableTreeNode otherNode = (DefaultMutableTreeNode) path.getLastPathComponent();
	    		if(node.getUserObject().getClass().isInstance(otherNode.getUserObject())) {
	    			if(node.getUserObject() instanceof Parameters) {
	    		    	DefaultMutableTreeNode entityNode = (DefaultMutableTreeNode) node.getParent();
	    		    	DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) entityNode.getParent();
		    			replaceItem.addActionListener(e -> {
			    	    	VPIDatas updatedData = controller.mergeParameter((FileDatas)fileDatasNode.getUserObject(), (Entity)entityNode.getUserObject(), (Parameters)node.getUserObject(), (Parameters)otherNode.getUserObject());
			    	    	
			    	    	// Mise à jour de l'arbre de droite
			                if(otherTree != null) {
			                    otherTree.update(updatedData, this.onlyDiff);
			                }
		    	    	});
	    			}
	    			else {
	    				DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) node.getParent();
	    				replaceItem.addActionListener(e -> {
			    	    	VPIDatas updatedData = controller.mergeEntity((FileDatas)fileDatasNode.getUserObject(), (Entity)node.getUserObject(), (Entity)otherNode.getUserObject());
			    	    	
			    	    	// Mise à jour de l'arbre de droite
			                if(otherTree != null) {
			                    otherTree.update(updatedData, this.onlyDiff);
			                }
		    	    	});
	    			}
	    			menu.add(replaceItem);
	    		}
	    	}
	    }

	    return menu;
	}

	public void synchronizeExpansion(TreePath sourcePath, boolean expand) {
	
	        TreePath targetPath = convertPathForTree(sourcePath);
	
	        if (targetPath != null) {
	            if (expand)
	                this.expandPath(targetPath);
	            else
	                this.collapsePath(targetPath);
	        }
	}
		
	private TreePath convertPathForTree(TreePath sourcePath) {
	
	    Object[] nodes = sourcePath.getPath();
	
	    TreeNode current = (TreeNode) this.getModel().getRoot();
	
	    // On commence à 1 pour ignorer la racine (déjà connue)
	    for (int i = 1; i < nodes.length; i++) {
	        TreeNode match = findMatchingChild(current, nodes[i]);
	        if (match == null)
	            return null;
	        current = match;
	    }
	
	    return new TreePath(((DefaultMutableTreeNode) current).getPath());
	}
	
	private TreeNode findMatchingChild(TreeNode parent, Object toMatch) {
	    int childCount = parent.getChildCount();

	    for (int i = 0; i < childCount; i++) {
	        TreeNode child = parent.getChildAt(i);

	        // Compare uniquement le nom du nœud (toString())
	        if (child.toString().equals(toMatch.toString())) {
	            return child;
	        }
	    }

	    return null; // pas trouvé
	}
	
	private void showInfo(Object data) {
		JTextArea textArea = new JTextArea();
		if(data instanceof VPIDatas)
			textArea = new JTextArea(((VPIDatas)data).getInfo());
		if(data instanceof FileDatas)
			textArea = new JTextArea(((FileDatas)data).getInfo());
		if(data instanceof Entity)
			textArea = new JTextArea(((Entity)data).getInfo());
		
	    textArea.setEditable(false);      // lecture seule
	    textArea.setLineWrap(true);        // retour à la ligne automatique
	    textArea.setWrapStyleWord(true);   // coupe proprement les mots

	    JScrollPane scrollPane = new JScrollPane(textArea);
	    scrollPane.setPreferredSize(new Dimension(500, 400)); // taille de la fenêtre

	    JOptionPane.showMessageDialog(
	        null,
	        scrollPane,
	        "Informations",
	        JOptionPane.INFORMATION_MESSAGE
	    );
	}
}
