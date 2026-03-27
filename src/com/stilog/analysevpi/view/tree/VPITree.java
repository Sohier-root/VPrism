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
import com.stilog.analysevpi.view.filter.FilterCompareDialog;
import com.stilog.analysevpi.view.filter.FilterDialog;
import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.vpimodel.objects.Attribute;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Mergeable;
import com.stilog.vpimodel.objects.Resolveable;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.VPIDatas;
import com.stilog.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.vpimodel.utils.FilterParser;
import com.stilog.vpimodel.vpsettings.FileDatas;
import com.stilog.vpimodel.vpsettings.Filter;

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

		// Menu contextuel uniquement pour l'arbre de référence
		if (!isRefTree) return menu;

		// Seulement Parameters et Entity
		if (!(obj instanceof Parameters || obj instanceof Entity)) return menu;

		// Seulement si l'objet a un problème (absent ou valeur différente)
		Resolveable resolveable = (Resolveable) obj;
		boolean hasAnomaly = resolveable.isAnomaly();
		boolean hasChanged = resolveable.isChanged();
		if (!hasAnomaly && !hasChanged) return menu;

		MergeRequestFactory factory = new MergeRequestFactory();

		if (obj instanceof Mergeable mergeable) {
			// Merge : uniquement pour les absents (anomaly), pas pour les valeurs différentes
			if (hasAnomaly && !hasChanged && mergeable.isMergeable()) {
				JMenuItem mergeItem = new JMenuItem("Merge");
				mergeItem.addActionListener(e -> executeMerge(node, factory));
				menu.add(mergeItem);
			}

			// Replace : disponible pour absents ET valeurs différentes,
			// mais pour "changed" uniquement avec le nœud correspondant de l'autre arbre
			if (mergeable.isReplaceable()) {
				if (hasChanged) {
					// Valeur différente : replace forcément avec le nœud correspondant (même nom)
					if (canPerformReplaceWithCorresponding(node)) {
						JMenuItem replaceItem = new JMenuItem("Replace");
						replaceItem.addActionListener(e -> executeReplaceWithCorresponding(node, factory));
						menu.add(replaceItem);
					}
				} else if (hasAnomaly && canPerformReplace(node)) {
					// Absent : replace libre avec la sélection de l'autre arbre
					JMenuItem replaceItem = new JMenuItem("Replace");
					replaceItem.addActionListener(e -> executeReplace(node, factory));
					menu.add(replaceItem);
				}
			}
		}

		return menu;
	}

	/**
	 * Exécute une opération de merge simple (pour les absents)
	 */
	private void executeMerge(DefaultMutableTreeNode node, MergeRequestFactory factory) {
		try {
			MergeRequest request = factory.createMergeRequest(node);
			VPIDatas updatedData = controller.performMerge(request);
			resolveNodeInModel(node);
			notifyTreeUpdate(updatedData);
		} catch (Exception ex) {
			showError("Erreur lors du merge", ex);
		}
	}

	/**
	 * Exécute un replace libre (pour les absents, sélection manuelle dans l'autre arbre)
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
			resolveNodeInModel(node);
			notifyTreeUpdate(updatedData);
		} catch (IllegalArgumentException ex) {
			showWarning(ex.getMessage());
		} catch (Exception ex) {
			showError("Erreur lors du replace", ex);
		}
	}

	/**
	 * Pour un nœud "changed" : vérifie que le nœud correspondant (même nom)
	 * existe dans l'autre arbre et est sélectionné, ou qu'on peut le trouver automatiquement.
	 * Le replace se fait toujours avec le nœud de même nom côté testé, sans besoin de sélection.
	 */
	private boolean canPerformReplaceWithCorresponding(DefaultMutableTreeNode node) {
		if (otherTree == null) return false;
		String name = nameOf(node.getUserObject());
		if (name == null) return false;
		return findNodeByName(otherTree, node.getUserObject().getClass(), name) != null;
	}

	/**
	 * Replace automatique pour un nœud "changed" :
	 * trouve le nœud correspondant dans l'autre arbre par nom et type, sans sélection manuelle.
	 * Après le replace, marque le nœud ref en resolve (vert) et rafraîchit les deux arbres.
	 */
	private void executeReplaceWithCorresponding(DefaultMutableTreeNode node, MergeRequestFactory factory) {
		try {
			String name = nameOf(node.getUserObject());
			DefaultMutableTreeNode otherNode = findNodeByName(otherTree, node.getUserObject().getClass(), name);
			if (otherNode == null) {
				showWarning("Nœud correspondant introuvable dans l'autre arbre.");
				return;
			}

			MergeRequest request = factory.createReplaceRequest(node, otherNode);
			VPIDatas updatedRight = controller.performMerge(request);

			// Marquer resolve sur le nœud ref (dans le modèle, pas juste dans l'arbre)
			resolveNodeInModel(node);

			// Rafraîchir les deux arbres
			VPIDatas refDatas = controller.getVPIData(com.stilog.vpimodel.objects.TypeFile.COMPARISON_LEFT);
			this.update(refDatas, this.onlyDiff);
			notifyTreeUpdate(updatedRight);

		} catch (IllegalArgumentException ex) {
			showWarning(ex.getMessage());
		} catch (Exception ex) {
			showError("Erreur lors du replace", ex);
		}
	}

	/**
	 * Marque un nœud "changed" comme résolu dans le modèle de données ref,
	 * puis remonte vers les parents pour recalculer leur état agrégé :
	 * - resolve (vert) si tous les enfants sont résolus ou neutres
	 * - changed (orange) s'il reste des enfants changed
	 * - anomaly (rouge) s'il reste des enfants anomaly
	 */
	private void resolveNodeInModel(DefaultMutableTreeNode node) {
		// 1. Descendre récursivement et marquer tous les enfants en resolve
		resolveSubtree(node);

		// 2. Remonter vers les parents et recalculer leur état agrégé
		javax.swing.tree.TreeNode parent = node.getParent();
		while (parent instanceof DefaultMutableTreeNode parentNode) {
			Object parentObj = parentNode.getUserObject();
			if (!(parentObj instanceof Resolveable r)) break;

			boolean anyAnomaly   = false;
			boolean anyChanged   = false;
			boolean anyUnresolved = false;

			for (int i = 0; i < parentNode.getChildCount(); i++) {
				Object childObj = ((DefaultMutableTreeNode) parentNode.getChildAt(i)).getUserObject();
				if (childObj instanceof Resolveable cr) {
					if      (cr.isAnomaly())             anyAnomaly    = true;
					else if (cr.isChanged())             anyChanged    = true;
					else if (!cr.isResolve())            anyUnresolved = true;
				}
			}

			r.setAnomaly(anyAnomaly);
			r.setChanged(!anyAnomaly && anyChanged);
			r.setResolve(!anyAnomaly && !anyChanged && !anyUnresolved);

			parent = parentNode.getParent();
		}
	}

	/**
	 * Marque récursivement le nœud et tous ses descendants en resolve.
	 */
	private void resolveSubtree(DefaultMutableTreeNode node) {
		Object obj = node.getUserObject();
		if (obj instanceof Resolveable r) {
			r.setChanged(false);
			r.setAnomaly(false);
			r.setResolve(true);
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			resolveSubtree((DefaultMutableTreeNode) node.getChildAt(i));
		}
	}

	/**
	 * Retourne le nom d'un objet du modèle (Parameters ou Entity).
	 */
	private String nameOf(Object obj) {
		if (obj instanceof Parameters p) return p.getName();
		if (obj instanceof Entity e)     return e.getName();
		return null;
	}

	/**
	 * Cherche dans un arbre le premier nœud d'un type et d'un nom donnés.
	 */
	private DefaultMutableTreeNode findNodeByName(VPITree tree, Class<?> type, String name) {
		Object root = tree.getModel().getRoot();
		if (!(root instanceof DefaultMutableTreeNode)) return null;
		java.util.Enumeration<?> nodes = ((DefaultMutableTreeNode) root).depthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			Object n = nodes.nextElement();
			if (!(n instanceof DefaultMutableTreeNode tn)) continue;
			Object obj = tn.getUserObject();
			if (type.isInstance(obj) && name.equals(nameOf(obj))) return tn;
		}
		return null;
	}

	/**
	 * Vérifie si on peut effectuer un replace (libre, pour les anomalies)
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
	 * Notifie la mise à jour des deux arbres après un merge/replace d'anomalie.
	 * Met à jour otherTree (testé) avec les données mergées,
	 * et rafraîchit this (ref) pour afficher la couleur resolve (vert).
	 */
	private void notifyTreeUpdate(VPIDatas updatedData) {
		if (otherTree != null) {
			otherTree.update(updatedData, this.onlyDiff);
		}
		// Rafraîchir l'arbre ref pour mettre à jour les couleurs (resolve → vert)
		VPIDatas refDatas = controller.getVPIData(com.stilog.vpimodel.objects.TypeFile.COMPARISON_LEFT);
		this.update(refDatas, this.onlyDiff);
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

	    // Récupérer le Parameters du nœud courant (ou premier enfant si Entity)
	    if (obj instanceof Entity) {
	    	if (node.getChildCount() == 0) return;
	    	obj = ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject();
	    }
	    if (!(obj instanceof Parameters)) return;

	    Parameters param = (Parameters) obj;
	    String conditionsXml = param.getAttributeValue(VPIConstants.PARAMETER_CONDITIONS);
	    if (conditionsXml == null || conditionsXml.isBlank()) return;

	    boolean isEventFilter = VPIConstants.NAME_TREE_EVENTSFILTER.equals(parentFilter.getName());

	    // Parser le filtre du côté courant
	    FilterGroupNode thisGroup = FilterParser.parse(conditionsXml, isEventFilter);

	    // Chercher le même filtre dans l'autre arbre
	    FilterGroupNode otherGroup = findCorrespondingFilter(param.getName(), isEventFilter);

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
	private FilterGroupNode findCorrespondingFilter(String filterName, boolean isEventFilter) {
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

	        String conditionsXml = p.getAttributeValue(VPIConstants.PARAMETER_CONDITIONS);
	        if (conditionsXml == null || conditionsXml.isBlank()) return null;
	        return FilterParser.parse(conditionsXml, isEventFilter);
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

