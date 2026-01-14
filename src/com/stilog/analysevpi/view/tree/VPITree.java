package com.stilog.analysevpi.view.tree;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.List;

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

import com.stilog.analysevpi.model.objects.Attribute;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.model.objects.VPIDatas;
import com.stilog.analysevpi.model.vpsettings.ResourceModel;
import com.stilog.analysevpi.model.vpsettings.FileDatas;
import com.stilog.analysevpi.model.vpsettings.Filter;

public class VPITree extends JTree{

	private static final long serialVersionUID = 1L;
	
	public VPITree() {
		super();
		DefaultMutableTreeNode VPIDatasNode = new DefaultMutableTreeNode("VPI Datas");
		this.setModel(new DefaultTreeModel(VPIDatasNode));
		
		JTree tree = this;
		this.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mousePressed(MouseEvent e) {
		        if (e.isPopupTrigger()) {
		            showPopup(e);
		        }
		    }

		    @Override
		    public void mouseReleased(MouseEvent e) {
		        if (e.isPopupTrigger()) {
		            showPopup(e);
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
	
	/*
	 * METHODS
	 */
	public void update(VPIDatas datas, boolean onlyDiff) {
		try {
			this.setCellRenderer(new CustomTreeCellRenderer());
			
			DefaultMutableTreeNode root = new DefaultMutableTreeNode(datas);
			
			List<Field> datasFiles = datas.getFilesDatas();
			
			for(int i = 0; i<datasFiles.size(); i++) {
				FileDatas fileDatas = (FileDatas) datasFiles.get(i).get(datas);
				if(fileDatas == null)
					continue;
				fileDatas.sort();
				DefaultMutableTreeNode fileNode = computeFileNode(fileDatas, onlyDiff);
				if(onlyDiff && fileNode.getChildCount() == 0)
					continue;
				
				root.add(fileNode);
			}
			
			this.setModel(new DefaultTreeModel(root));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private DefaultMutableTreeNode computeFileNode(FileDatas datas, boolean onlyDiff) {
		
		DefaultMutableTreeNode datasNode = new DefaultMutableTreeNode(datas);
		
		for(Entity entity : datas.getEntities()) {
			DefaultMutableTreeNode entityNode = new DefaultMutableTreeNode(entity);
			
			if(onlyDiff && !entity.isAnomaly())
				continue;

			for(Parameters param : entity.getParameters()) {
				DefaultMutableTreeNode paramNode = new DefaultMutableTreeNode(param);
				
				if(onlyDiff && !param.isAnomaly())
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
	
	private boolean nodeAllowsContextMenu(DefaultMutableTreeNode node) {
	    return node.getUserObject() instanceof Entity || 
	    		node.getUserObject() instanceof VPIDatas ||
	    		node.getUserObject() instanceof FileDatas;
	}
	
	private JPopupMenu createPopupMenuForNode(DefaultMutableTreeNode node) {

	    Object obj = node.getUserObject();

	    JPopupMenu menu = new JPopupMenu();

	    JMenuItem infoItem = new JMenuItem("Afficher info");
	    infoItem.addActionListener(e -> showInfo(obj));
	    menu.add(infoItem);

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
