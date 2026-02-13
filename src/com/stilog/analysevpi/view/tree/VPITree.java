package com.stilog.analysevpi.view.tree;

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

import com.stilog.analysevpi.controller.ComparisonController;
import com.stilog.analysevpi.model.dto.MergeRequest;
import com.stilog.vpimodel.objects.Attribute;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Mergeable;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.VPIDatas;
import com.stilog.vpimodel.vpsettings.FileDatas;

public class VPITree extends JTree {

	private static final long serialVersionUID = 1L;

	private ComparisonController controller;
	private VPITree otherTree;

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

			if (this.onlyDiff && !entity.isAnomaly())
				continue;

			for (Parameters param : entity.getParameters()) {
				DefaultMutableTreeNode paramNode = new DefaultMutableTreeNode(param);

				if (this.onlyDiff && !param.isAnomaly())
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

		// Menu contextuel uniquement pour l'arbre de référence
		if (!isRefTree) {
			return menu;
		}

		// Vérifier si le type supporte les opérations de merge
		if (!(obj instanceof Parameters || obj instanceof Entity)) {
			return menu;
		}

		// Factory pour créer les requêtes
		MergeRequestFactory factory = new MergeRequestFactory();

		// ✅ Item "Merge" - simplifié
		JMenuItem mergeItem = new JMenuItem("Merge");
		mergeItem.addActionListener(e -> executeMerge(node, factory));
		if(node.getUserObject() instanceof Mergeable && 
				((Mergeable)node.getUserObject()).isMergeable()) {
			menu.add(mergeItem);
		}

		// ✅ Item "Replace" - simplifié
		if (canPerformReplace(node)) {
			JMenuItem replaceItem = new JMenuItem("Replace");
			replaceItem.addActionListener(e -> executeReplace(node, factory));
			if(node.getUserObject() instanceof Mergeable && 
					((Mergeable)node.getUserObject()).isReplaceable()) {
				menu.add(replaceItem);
			}
		}

		return menu;
	}

	/**
	 * Exécute une opération de merge simple
	 */
	private void executeMerge(DefaultMutableTreeNode node, MergeRequestFactory factory) {
		try {
			MergeRequest request = factory.createMergeRequest(node);
			VPIDatas updatedData = controller.performMerge(request);
			notifyTreeUpdate(updatedData);
		} catch (Exception ex) {
			showError("Erreur lors du merge", ex);
		}
	}

	/**
	 * Exécute une opération de replace
	 */
	private void executeReplace(DefaultMutableTreeNode node, MergeRequestFactory factory) {
		try {
			TreePath otherPath = otherTree.getSelectionPath();
			if (otherPath == null) {
				showWarning("Aucune sélection dans l'autre arbre");
				return;
			}

			DefaultMutableTreeNode otherNode = (DefaultMutableTreeNode) otherPath.getLastPathComponent();

			MergeRequest request = factory.createReplaceRequest(node, otherNode);
			VPIDatas updatedData = controller.performMerge(request);
			notifyTreeUpdate(updatedData);
		} catch (IllegalArgumentException ex) {
			showWarning(ex.getMessage());
		} catch (Exception ex) {
			showError("Erreur lors du replace", ex);
		}
	}

	/**
	 * Vérifie si on peut effectuer un replace
	 */
	private boolean canPerformReplace(DefaultMutableTreeNode node) {
		if (this.isSelectionEmpty() || otherTree.isSelectionEmpty()) {
			return false;
		}

		TreePath path = otherTree.getSelectionPath();
		DefaultMutableTreeNode otherNode = (DefaultMutableTreeNode) path.getLastPathComponent();

		// Vérifier que les types correspondent
		return node.getUserObject().getClass().equals(otherNode.getUserObject().getClass());
	}

	/**
	 * Notifie la mise à jour de l'arbre (pattern Observer simplifié)
	 */
	private void notifyTreeUpdate(VPIDatas updatedData) {
		if (otherTree != null) {
			otherTree.update(updatedData, this.onlyDiff);
		}
	}

	/**
	 * Affiche un message d'erreur
	 */
	private void showError(String message, Exception ex) {
		ex.printStackTrace();
		JOptionPane.showMessageDialog(this, message + " : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Affiche un avertissement
	 */
	private void showWarning(String message) {
		JOptionPane.showMessageDialog(this, message, "Attention", JOptionPane.WARNING_MESSAGE);
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
}
