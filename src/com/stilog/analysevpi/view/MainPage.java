package com.stilog.analysevpi.view;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

import com.stilog.analysevpi.controller.Controller;
import com.stilog.analysevpi.model.objects.PositionFile;
import com.stilog.analysevpi.utils.SystemInfo;
import com.stilog.analysevpi.view.documentation.DocumentationWindow;
import com.stilog.analysevpi.view.loading.LoadingIcon;
import com.stilog.analysevpi.view.loading.LoadingWindow;
import com.stilog.analysevpi.view.object.VButton;
import com.stilog.analysevpi.view.tree.VPITree;

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Exemple d'interface Swing avec deux zones (gauche / droite). Chaque zone
 * permet d'importer un fichier et d'exécuter une fonction sur ce fichier.
 *
 * Pour compiler : javac InterfaceSwing_DoubleZone.java Pour exécuter : java
 * InterfaceSwing_DoubleZone
 */
public class MainPage extends JFrame {
	/*
	 * CONSTANTS
	 */
	private static final String TOOLTIP_SYNCHRONIZE = "Synchroniser les affichages";
	private static final String TOOLTIP_DESYNCHRONIZE = "Désynchroniser les affichages";
	private static final String TOOLTIP_DISPLAY_DIFFONLY = "Afficher seulement les différences";
	private static final String TOOLTIP_DISPLAY_ALL = "Tout afficher";
	private static final String TOOLTIP_GENERATE_VPI = "Generer un nouveau VPI/VPS";
	private static final String TOOLTIP_REVERSE = "Intervertir les affichages";
	
	/*
	 * VARIABLES
	 */
	private Controller controller;

	private SystemInfo infos = SystemInfo.getInstance();

	/*
	 * Zone VPI Gauche (Référence)
	 */
	private JTextField leftFileField;
	private JButton browseBtnLeft;
	private VPITree treeLeft;
	private File leftSelectedFile;
	private JScrollPane scrollPanelLeft;

	/*
	 * Zone VPI Droite (Testé)
	 */
	private JTextField rightFileField;
	private JButton browseBtnRight;
	private VPITree treeRight;
	private File rightSelectedFile;
	private JScrollPane scrollPanelRight;

	/*
	 * Zone Sud
	 */

	private VButton compareBtn;

	/*
	 * Toolbar
	 */
	private JToggleButton synchronizeBtn;
	private JToggleButton diffOnlyBtn;
	private JButton generateFilesBtn;

	private JButton helpBtn;
	private JButton reverseDatasBtn;
	
	/*
	 * Icônes pour le bouton Generate Files
	 */
	private FontIcon generateFilesIconNormal;
	private LoadingIcon generateFilesIconLoading;

	public MainPage(Controller controller) {
		super("VP Comparator");
		this.setTitle(this.getTitle() + " (" + infos.getVersion() + ")");
		this.controller = controller;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 600);
		setLocationRelativeTo(null);

		// Split pane horizontal (gauche / droite)
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setResizeWeight(0.5);

		// Panels gauche et droite
		JPanel leftPanel = createZonePanel("Référence", true);
		JPanel rightPanel = createZonePanel("Testé", false);

		JPanel commonPanel = createCommonPanel("Sud");

		splitPane.setLeftComponent(leftPanel);
		splitPane.setRightComponent(rightPanel);

		getContentPane().add(createToolbarPanel(), BorderLayout.NORTH);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		getContentPane().add(commonPanel, BorderLayout.SOUTH);

		this.initSynchro();
		this.initializeDiffOnlyToggle();
		this.initializeGenerateFiles();
		this.initializeReverseDatas();
	}

	private JPanel createZonePanel(String title, boolean isLeft) {
		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createTitledBorder(title));

		// Top: file chooser controls
		JPanel top = new JPanel(new BorderLayout(6, 6));
		JTextField fileField = new JTextField();
		fileField.setEditable(false);
		JButton browseBtn = new JButton("Importer...");
		browseBtn.putClientProperty("JButton.buttonType", "roundRect");
		browseBtn.setEnabled(isLeft);

		top.add(fileField, BorderLayout.CENTER);
		top.add(browseBtn, BorderLayout.EAST);

		// Tree
		VPITree tree = new VPITree(this.controller, isLeft);
		JScrollPane scroll = new JScrollPane(tree);

		// Bottom: execute button
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		panel.add(top, BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(bottom, BorderLayout.SOUTH);

		// Link fields to left/right variables
		if (isLeft) {
			this.leftFileField = fileField;
			this.treeLeft = tree;
			this.browseBtnLeft = browseBtn;
			this.scrollPanelLeft = scroll;
		} else {
			this.rightFileField = fileField;
			this.treeRight = tree;
			this.treeRight.setOtherTree(this.treeLeft);
			this.treeLeft.setOtherTree(this.treeRight);
			this.browseBtnRight = browseBtn;
			this.scrollPanelRight = scroll;
		}

		// Browse action
		browseBtn.addActionListener(e -> {
			chooseVPIFile(isLeft);
			LoadingWindow.run(this, () -> {
				processFile(isLeft);
				if (isLeft) {
					this.browseBtnRight.setEnabled(true);
				}
			});
		});

		return panel;
	}

	private JPanel createCommonPanel(String title) {
		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


		VButton compareBtn = new VButton("Comparer");
		compareBtn.putClientProperty("JButton.buttonType", "roundRect");
		compareBtn.setEnabled(false);
		panel.add(compareBtn, BorderLayout.CENTER);

		compareBtn.addActionListener(e -> {
			LoadingWindow.run(this, () -> {
				treeLeft.update(controller.performComparison(), diffOnlyBtn.isSelected());
				diffOnlyBtn.setEnabled(true);
			});
		});

		this.compareBtn = compareBtn;
		return panel;
	}

	private JPanel createToolbarPanel() {
		JToolBar toolbar = createToolbar();

		// Panel pour centrer
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		topPanel.add(toolbar);

		return topPanel;
	}

	private void chooseVPIFile(Boolean isLeft) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// Optional: set filters
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Fichiers VPI/VPS", "vpi", "vps");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(isLeft ? leftSelectedFile : rightSelectedFile);
		int res = chooser.showOpenDialog(this);
		if (res == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			if (isLeft) {
				leftSelectedFile = f;
				leftFileField.setText(f.getAbsolutePath());
			} else {
				rightSelectedFile = f;
				rightFileField.setText(f.getAbsolutePath());
			}
		}
	}
	
	private String chooseVPIFolder() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Dossier cible");
		int res = chooser.showOpenDialog(this);
		if (res == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			return f.getAbsolutePath();
		}
		return null;
	}

	public void processFile(boolean isLeft) {
		try {
		if (isLeft) {
			if (leftSelectedFile == null) {
				JOptionPane.showMessageDialog(this, "Aucun fichier sélectionné (gauche).", "Erreur",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			// Appel vers fonction de traitement

				treeLeft.update(controller.handleFile(leftSelectedFile, PositionFile.LEFT), diffOnlyBtn.isSelected());
			}
				diffOnlyBtn.setSelected(false);
				diffOnlyBtn.setEnabled(false);
		} else {
			if (rightSelectedFile == null) {
				JOptionPane.showMessageDialog(this, "Aucun fichier sélectionné (droite).", "Erreur",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

				// Désactiver le bouton generate et afficher l'icône de chargement
				setGenerateFilesButtonLoading(true);
				
				diffOnlyBtn.setSelected(false);
				diffOnlyBtn.setEnabled(false);
				
				treeRight.update(controller.handleFile(rightSelectedFile, PositionFile.RIGHT),
						diffOnlyBtn.isSelected());
				
				treeLeft.update(controller.getVPIData(PositionFile.LEFT), diffOnlyBtn.isSelected());

				// Récupérer le CompletableFuture pour savoir quand le dézipage est terminé
				CompletableFuture<Boolean> unzipFuture = controller.getCompleteUnzipFuture(rightSelectedFile,
						PositionFile.RIGHT);
				
				// Quand le dézipage est terminé, réactiver le bouton
				unzipFuture.thenAccept(success -> {
					SwingUtilities.invokeLater(() -> {
						setGenerateFilesButtonLoading(false);
						if (!success) {
							System.err.println("Erreur lors du dézipage complet");
						}
					});
				});
			}
				
			setGenerateFilesButtonLoading(false);
			if (controller.getVPIData(PositionFile.RIGHT).isParsed()
					&& controller.getVPIData(PositionFile.LEFT).isParsed()) {
				this.compareBtn.setEnabled(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Active ou désactive le mode chargement du bouton Generate Files
	 * @param loading true pour afficher le chargement, false pour l'icône normale
	 */
	private void setGenerateFilesButtonLoading(boolean loading) {
		if (loading) {
			// Désactiver le bouton et afficher l'icône de chargement
			generateFilesBtn.setEnabled(false);
			generateFilesBtn.setIcon(generateFilesIconLoading);
			generateFilesIconLoading.start(generateFilesBtn);
		} else {
			// Réactiver le bouton et afficher l'icône normale
			generateFilesIconLoading.stop();
			generateFilesBtn.setIcon(generateFilesIconNormal);
			generateFilesBtn.setEnabled(true);
		}
	}

	/**
	 * Création de la barre de menu
	 * @return
	 */
	private JToolBar createToolbar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false); // Pour éviter le détachement moche
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	    /*
	     * Bouton Help (tout à gauche)
	     */
	    this.helpBtn = new JButton(FontIcon.of(MaterialDesign.MDI_HELP_CIRCLE, 20));
	    this.helpBtn.setToolTipText("Aide et documentation");
	    this.helpBtn.addActionListener(e -> openDocumentation());
	    
		/*
		 * Boutton Synchro
		 */
		this.synchronizeBtn = new JToggleButton(FontIcon.of(MaterialDesign.MDI_SYNC, 20));

		/*
		 * Boutton DiffOnly
		 */
		this.diffOnlyBtn = new JToggleButton(FontIcon.of(MaterialDesign.MDI_VECTOR_DIFFERENCE, 20));
		this.diffOnlyBtn.setEnabled(false);
		this.diffOnlyBtn.setToolTipText(TOOLTIP_DISPLAY_DIFFONLY);
		
		/*
		 * Boutton Generate Files
		 */
		this.generateFilesIconNormal = FontIcon.of(MaterialDesign.MDI_FILE_CHECK, 20);
		this.generateFilesIconLoading = new LoadingIcon();
		this.generateFilesBtn = new JButton(generateFilesIconNormal);
		this.generateFilesBtn.setToolTipText(TOOLTIP_GENERATE_VPI);
		this.generateFilesBtn.setEnabled(false); // Désactivé par défaut

		/*
		 * Bouton Reverse
		 */
		this.reverseDatasBtn = new JButton(FontIcon.of(MaterialDesign.MDI_SWAP_HORIZONTAL, 20));
		this.reverseDatasBtn.setToolTipText(TOOLTIP_REVERSE);

		/*
		 * Ajout a la toolbar
		 */
		toolbar.add(helpBtn, BorderLayout.WEST);
		toolbar.addSeparator();
		toolbar.add(diffOnlyBtn, BorderLayout.CENTER);
		toolbar.add(synchronizeBtn, BorderLayout.CENTER);
		toolbar.add(generateFilesBtn, BorderLayout.CENTER);
		toolbar.add(reverseDatasBtn, BorderLayout.CENTER);

		return toolbar;
	}

	/**
	 * Initialisation
	 */
	private void initSynchro() {
		// Listener for synchro
		TreeExpansionListener listenerTreeLeft = createTreeExpansionListener(treeRight);
		TreeExpansionListener listenerTreeRight = createTreeExpansionListener(treeLeft);

		// Listener ScrollBar
		JScrollBar scrollLeft = scrollPanelLeft.getVerticalScrollBar();
		JScrollBar scrollRight = scrollPanelRight.getVerticalScrollBar();
		AdjustmentListener listenerScrollLeft = createScrollListener(scrollRight);
		AdjustmentListener listenerScrollRight = createScrollListener(scrollLeft);

		// Synchroniser par défault
		this.synchronizeBtn.setSelected(true);
		enableSynchronization(listenerTreeLeft, listenerTreeRight, listenerScrollLeft, listenerScrollRight);

		// Toggle au clic
		synchronizeBtn.addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
        	enableSynchronization(listenerTreeLeft, listenerTreeRight, listenerScrollLeft, listenerScrollRight);
        } else {
        	disableSynchronization(listenerTreeLeft, listenerTreeRight, listenerScrollLeft, listenerScrollRight);
        }
    });
	}

	private TreeExpansionListener createTreeExpansionListener(VPITree targetTree) {
		return new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				targetTree.synchronizeExpansion(event.getPath(), true);
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				targetTree.synchronizeExpansion(event.getPath(), false);
			}
		};
	}

	private AdjustmentListener createScrollListener(JScrollBar targetScrollBar) {
		return e -> targetScrollBar.setValue(((JScrollBar) e.getSource()).getValue());
	}

	private void enableSynchronization(TreeExpansionListener leftTree, TreeExpansionListener rightTree,
			AdjustmentListener leftScroll, AdjustmentListener rightScroll) {
		treeLeft.addTreeExpansionListener(leftTree);
		treeRight.addTreeExpansionListener(rightTree);
		scrollPanelLeft.getVerticalScrollBar().addAdjustmentListener(leftScroll);
		scrollPanelRight.getVerticalScrollBar().addAdjustmentListener(rightScroll);
		
		this.synchronizeBtn.setIcon(FontIcon.of(MaterialDesign.MDI_SYNC, 20));
		this.synchronizeBtn.setToolTipText(TOOLTIP_DESYNCHRONIZE);
	}

	private void disableSynchronization(TreeExpansionListener leftTree, TreeExpansionListener rightTree,
			AdjustmentListener leftScroll, AdjustmentListener rightScroll) {
		treeLeft.removeTreeExpansionListener(leftTree);
		treeRight.removeTreeExpansionListener(rightTree);
		scrollPanelLeft.getVerticalScrollBar().removeAdjustmentListener(leftScroll);
		scrollPanelRight.getVerticalScrollBar().removeAdjustmentListener(rightScroll);
		
		this.synchronizeBtn.setIcon(FontIcon.of(MaterialDesign.MDI_SYNC_OFF, 20));
		this.synchronizeBtn.setToolTipText(TOOLTIP_SYNCHRONIZE);
	}

	private void initializeDiffOnlyToggle() {
		this.diffOnlyBtn.addItemListener(e -> {
	        if (e.getStateChange() == ItemEvent.SELECTED) {
	        	diffOnlyBtn.setToolTipText(TOOLTIP_DISPLAY_ALL);
	        	diffOnlyBtn.setSelected(true);
	        } else {
	        	diffOnlyBtn.setToolTipText(TOOLTIP_DISPLAY_DIFFONLY);
	        	diffOnlyBtn.setSelected(false);
	        }
	        treeLeft.update(controller.getVPIData(PositionFile.LEFT), diffOnlyBtn.isSelected());
		});
			}

	private void initializeGenerateFiles() {
		this.generateFilesBtn.addActionListener(e -> {
			String outputPath = chooseVPIFolder();
			// Vérifier que le bouton est activé
			if (generateFilesBtn.isEnabled() && outputPath != null) {
				LoadingWindow.run(this, () -> {
					controller.performGenerateVPI(outputPath);
				});
			}
		});
	}
	
	private void initializeReverseDatas() {
		this.reverseDatasBtn.addActionListener(e -> {
			//Reset de la toolbar
			this.diffOnlyBtn.setSelected(false);
			
			//Datas and trees
			controller.performReverse();
			treeLeft.update(controller.getVPIData(PositionFile.LEFT), diffOnlyBtn.isSelected());
			treeRight.update(controller.getVPIData(PositionFile.RIGHT), diffOnlyBtn.isSelected());
			
			//File
			String oldLeft = this.leftFileField.getText();
			this.leftFileField.setText(this.rightFileField.getText());
			this.rightFileField.setText(oldLeft);
			
			File oldLeftFile = this.leftSelectedFile;
			this.leftSelectedFile = this.rightSelectedFile;
			this.rightSelectedFile = oldLeftFile;
		});
	}

	/**
	 * Ouvre la fenêtre de documentation
	 */
	private void openDocumentation() {
	    DocumentationWindow docWindow = new DocumentationWindow();
	    docWindow.setVisible(true);

	
	@Override
	public void dispose() {
		// Arrêter l'animation si elle est en cours
		if (generateFilesIconLoading != null) {
			generateFilesIconLoading.stop();
		}
		super.dispose();
	}
}