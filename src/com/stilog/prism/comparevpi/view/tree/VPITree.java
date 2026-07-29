package com.stilog.prism.comparevpi.view.tree;

import java.awt.Dimension;
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

import com.stilog.prism.comparevpi.controller.ComparisonController;
import com.stilog.prism.comparevpi.view.filter.FilterCompareDialog;
import com.stilog.prism.comparevpi.view.filter.FilterDialog;
import com.stilog.prism.vpimodel.objects.Attribute;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.VPIDatas;
import com.stilog.prism.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.prism.vpimodel.reader.FilterConditionFormatter;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;
import com.stilog.prism.vpimodel.vpsettings.Filter;
import com.visualplanning.vpi.model.filter.FilterCondition;

public class VPITree extends JTree {

	private static final long serialVersionUID = 1L;

	private ComparisonController controller;
	private VPITree otherTree;
	private TreeSearchBar searchBar;

	boolean isRefTree = false;
	boolean onlyDiff = false;

	public VPITree(ComparisonController controller, boolean isRefTree) {
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
				} else {
					if (!e.isControlDown())
						otherTree.clearSelection();
					if (e.getClickCount() == 2) {
						TreePath path = tree.getPathForLocation(e.getX(), e.getY());
						if (path != null) {
							DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
							tryOpenFilterDialog(node, e);
						}
					}
				}
			}

			private void showPopup(MouseEvent e) {
				int row = tree.getRowForLocation(e.getX(), e.getY());
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());

				if (path == null)
					return;

				tree.setSelectionPath(path); // sélection du nœud cliqué

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

	/** Barre de recherche associée à cet arbre, rafraîchie automatiquement à chaque {@link #update}. */
	public void setSearchBar(TreeSearchBar searchBar) {
		this.searchBar = searchBar;
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

			List<FileDatas> datasFiles = datas.getFilesDatas();

			for (int i = 0; i < datasFiles.size(); i++) {
				FileDatas fileDatas = datasFiles.get(i);
				if (fileDatas == null)
					continue;
				fileDatas.sort();
				DefaultMutableTreeNode fileNode = computeFileNode(fileDatas);
				if (onlyDiff && fileNode.getChildCount() == 0)
					continue;

				root.add(fileNode);
			}

			this.setModel(new DefaultTreeModel(root));

			// Restaurer l'état d'expansion après la mise à jour
			restoreExpandedPaths(expandedPaths);

			if (searchBar != null)
				searchBar.refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sauvegarde tous les chemins actuellement expansés dans l'arbre
	 */
	private void saveExpandedPaths(Set<TreePath> expandedPaths) {
		TreeNode root = (TreeNode) this.getModel().getRoot();
		if (root == null)
			return;

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
	 * Trouve le chemin équivalent dans le nouveau modèle d'arbre en comparant les
	 * toString() des nœuds
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

		for (Entity entity : datas.getEntities()) {
			DefaultMutableTreeNode entityNode = new DefaultMutableTreeNode(entity);

			if (this.onlyDiff && !entity.isAnomaly() && !entity.isChanged())
				continue;

			for (Parameters param : entity.getParameters()) {
				DefaultMutableTreeNode paramNode = new DefaultMutableTreeNode(param);

				if (this.onlyDiff && !param.isAnomaly() && !param.isChanged())
					continue;

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
	 * Noeud autorisé a afficher le menu contextuel
	 * 
	 * @param node
	 * @return
	 */
	private boolean nodeAllowsContextMenu(DefaultMutableTreeNode node) {
		return node.getUserObject() instanceof Entity || node.getUserObject() instanceof VPIDatas
				|| node.getUserObject() instanceof FileDatas || node.getUserObject() instanceof Parameters;
	}

	private JPopupMenu createPopupMenuForNode(DefaultMutableTreeNode node) {
		Object obj = node.getUserObject();
		JPopupMenu menu = new JPopupMenu();

		// Item "Afficher info" - commun à tous
		JMenuItem infoItem = new JMenuItem("Afficher info");
		infoItem.addActionListener(e -> showInfo(obj));
		menu.add(infoItem);

		if (computeFilterNode(node) != null) {
			JMenuItem filterInfo = new JMenuItem("Détails du filtre");
			filterInfo.addActionListener(e -> tryOpenFilterDialog(node, null));
			menu.add(filterInfo);
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
		if (data instanceof VPIDatas)
			textArea = new JTextArea(((VPIDatas) data).getInfo());
		if (data instanceof FileDatas)
			textArea = new JTextArea(((FileDatas) data).getInfo());
		if (data instanceof Entity)
			textArea = new JTextArea(((Entity) data).getInfo());
		if (data instanceof Parameters)
			textArea = new JTextArea(((Parameters) data).getInfo());

		textArea.setEditable(false); // lecture seule
		textArea.setLineWrap(true); // retour à la ligne automatique
		textArea.setWrapStyleWord(true); // coupe proprement les mots

		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(500, 400)); // taille de la fenêtre

		JOptionPane.showMessageDialog(null, scrollPane, "Informations", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Ouvre le dialog de filtre approprié :
	 * - FilterCompareDialog (côte à côte, non modale) si les deux VPI sont chargés
	 *   et que le filtre existe des deux côtés.
	 * - FilterDialog (filtre seul, modale) si l'autre VPI n'est pas chargé
	 *   ou si le filtre est absent de l'autre côté.
	 */
	private void tryOpenFilterDialog(DefaultMutableTreeNode node, MouseEvent e) {
	    Object obj = node.getUserObject();

	    Filter parentFilter = computeFilterNode(node);
	    if (parentFilter == null) return;

	    // Récupérer l'Entity (nœud courant, ou parent direct si on est déjà sur le Parameters)
	    Entity entity;
	    if (obj instanceof Entity ent) {
	    	entity = ent;
	    	if (node.getChildCount() == 0) return;
	    	obj = ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject();
	    } else if (obj instanceof Parameters
	    		&& ((DefaultMutableTreeNode) node.getParent()).getUserObject() instanceof Entity parentEntity) {
	    	entity = parentEntity;
	    } else {
	    	return;
	    }
	    if (!(obj instanceof Parameters)) return;

	    Parameters param = (Parameters) obj;

	    FilterCondition.LogicGroup root = parentFilter.getRootCondition(entity).orElse(null);
	    if (root == null) return;

	    // Convertit le filtre du côté courant
	    FilterGroupNode thisGroup = FilterConditionFormatter.toFilterGroupNode(root, parentFilter.getResourceLabelResolver());

	    // Cherche le même filtre dans l'autre arbre
	    FilterGroupNode otherGroup = findCorrespondingFilter(param.getName());

	    if (otherGroup != null) {
	        // Les deux filtres existent : affichage côte à côte
	        FilterGroupNode refGroup  = isRefTree ? thisGroup  : otherGroup;
	        FilterGroupNode testGroup = isRefTree ? otherGroup : thisGroup;
	        FilterCompareDialog dialog = new FilterCompareDialog(this, param.getName(), refGroup, testGroup);
	        dialog.setVisible(true);
	    } else {
	        // Un seul filtre disponible : affichage simple (modale)
	        FilterDialog dialog = new FilterDialog(this, param.getName(), thisGroup);
	        dialog.setVisible(true);
	    }
	}

	/**
	 * Cherche dans l'autre arbre un filtre de même nom et retourne son FilterGroupNode parsé.
	 * Retourne null si l'autre arbre n'est pas chargé ou si le filtre est absent.
	 */
	private FilterGroupNode findCorrespondingFilter(String filterName) {
	    if (otherTree == null) return null;

	    javax.swing.tree.TreeModel model = otherTree.getModel();
	    if (model == null) return null;

	    Object root = model.getRoot();
	    if (!(root instanceof DefaultMutableTreeNode)) return null;

	    // Parcourir tous les nœuds de l'autre arbre pour trouver un Parameters
	    // dont le nom correspond et dont le parent est un Filter
	    java.util.Enumeration<?> nodes = ((DefaultMutableTreeNode) root).depthFirstEnumeration();
	    while (nodes.hasMoreElements()) {
	        Object nodeObj = nodes.nextElement();
	        if (!(nodeObj instanceof DefaultMutableTreeNode)) continue;
	        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) nodeObj;
	        Object userObj = treeNode.getUserObject();

	        if (!(userObj instanceof Parameters)) continue;
	        Parameters p = (Parameters) userObj;
	        if (!filterName.equals(p.getName())) continue;

	        // Vérifier que ce Parameters appartient bien à un Filter
	        Filter f = otherTree.computeFilterNode(treeNode);
	        if (f == null) continue;

	        Object parentObj = ((DefaultMutableTreeNode) treeNode.getParent()).getUserObject();
	        if (!(parentObj instanceof Entity otherEntity)) return null;

	        return f.getRootCondition(otherEntity)
	                .map(cond -> FilterConditionFormatter.toFilterGroupNode(cond, f.getResourceLabelResolver()))
	                .orElse(null);
	    }
	    return null;
	}
	
	Filter computeFilterNode(DefaultMutableTreeNode node) {
	    Object obj = node.getUserObject();
	    if (!(obj instanceof Parameters) && !(obj instanceof Entity)) return null;

	    // Remonter l'arbre jusqu'à trouver un nœud FileDatas de type Filter
	    Filter parentFilter = null;
	    javax.swing.tree.TreeNode current = node.getParent();
	    while (current instanceof DefaultMutableTreeNode) {
	        Object currentObj = ((DefaultMutableTreeNode) current).getUserObject();
	        if (currentObj instanceof Filter) {
	            parentFilter = (Filter) currentObj;
	            break;
	        }
	        current = current.getParent();
	    }
	    return parentFilter;
	}
}

