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
import com.stilog.analysevpi.view.loading.LoadingWindow;
import com.stilog.analysevpi.view.tree.VPITree;

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

/**
 * Exemple d'interface Swing avec deux zones (gauche / droite).
 * Chaque zone permet d'importer un fichier et d'exécuter une fonction sur ce fichier.
 *
 * Pour compiler : javac InterfaceSwing_DoubleZone.java
 * Pour exécuter : java InterfaceSwing_DoubleZone
 */
public class MainPage extends JFrame {
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
    private JButton compareBtn;
    
    /*
     * Toolbar
     */
    private JButton synchronizeBtn;
    private JButton diffOnlyBtn;
    
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
        this.initDiffOnly();
    }

    private JPanel createZonePanel(String title, boolean isLeft) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        // Top: file chooser controls
        JPanel top = new JPanel(new BorderLayout(6, 6));
        JTextField fileField = new JTextField();
        fileField.setEditable(false);
        JButton browseBtn = new JButton("Importer...");
        browseBtn.putClientProperty( "JButton.buttonType", "roundRect" );
        browseBtn.setEnabled(isLeft);

        top.add(fileField, BorderLayout.CENTER);
        top.add(browseBtn, BorderLayout.EAST);
        
        //Tree
        VPITree tree = new VPITree();
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
            this.browseBtnRight = browseBtn;
            this.scrollPanelRight = scroll;
        }

        // Browse action
        browseBtn.addActionListener(e -> {
            chooseFile(isLeft);
            LoadingWindow.run(this, () -> {
	            processFile(isLeft);
	            if(isLeft) {
	            	this.browseBtnRight.setEnabled(true);
	            }
	            else {
	            	this.compareBtn.setEnabled(true);
	            }
            });
        });

        return panel;
    }
    
    private JPanel createCommonPanel(String title) {
    	 JPanel panel = new JPanel(new BorderLayout(8, 8));
    	 
    	 JButton compareBtn = new JButton("Comparer");
    	 compareBtn.putClientProperty("JButton.buttonType", "roundRect");
    	 compareBtn.setEnabled(false);
    	 panel.add(compareBtn, BorderLayout.CENTER);
    	 
    	 compareBtn.addActionListener(e -> {
    		 LoadingWindow.run(this, () -> {
    			 treeLeft.update(controller.performComparison(), diffOnlyBtn.isSelected());
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
    
    private void chooseFile(boolean isLeft) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        // Optional: set filters
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Fichiers VPI/VPS", "vpi", "vps");
        chooser.setFileFilter(filter);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (isLeft) {
            	leftSelectedFile = f;
            	leftFileField.setText(f.getAbsolutePath()); 
            }
            else {
            	rightSelectedFile = f;
            	rightFileField.setText(f.getAbsolutePath());
            }
        }
    }
    
    public void processFile(boolean isLeft) {
        if (isLeft) {
            if (leftSelectedFile == null) {
                JOptionPane.showMessageDialog(this, "Aucun fichier sélectionné (gauche).", "Erreur", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Appel vers fonction de traitement
            try {
                treeLeft.update(controller.handleFile(leftSelectedFile, PositionFile.LEFT), diffOnlyBtn.isSelected());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            if (rightSelectedFile == null) {
                JOptionPane.showMessageDialog(this, "Aucun fichier sélectionné (droite).", "Erreur", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                treeRight.update(controller.handleFile(rightSelectedFile, PositionFile.RIGHT), diffOnlyBtn.isSelected());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false); // Pour éviter le détachement moche
        toolbar.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        /*
         * Boutton Synchro
         */
        this.synchronizeBtn = new JButton(FontIcon.of(MaterialDesign.MDI_SWAP_HORIZONTAL, 20));
        
        /*
         * Boutton DiffOnly
         */
        this.diffOnlyBtn = new JButton(FontIcon.of(MaterialDesign.MDI_VECTOR_DIFFERENCE, 20));
        
        /*
         * ToolTip
         */
        this.synchronizeBtn.setToolTipText("Synchroniser les affichages");
        this.diffOnlyBtn.setToolTipText("Afficher seulement les différences");
        
        /*
         *  Ajout a la toolbar
         */
        toolbar.add(diffOnlyBtn, BorderLayout.CENTER);
        toolbar.add(synchronizeBtn, BorderLayout.CENTER);

        return toolbar;
    }
    
    private void initSynchro() {
        //Listener for synchro
        TreeExpansionListener listenerTreeLeft = new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                treeRight.synchronizeExpansion(event.getPath(), true);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                treeRight.synchronizeExpansion(event.getPath(), false);
            }
        };
		TreeExpansionListener listenerTreeRight = new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                treeLeft.synchronizeExpansion(event.getPath(), true);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                treeLeft.synchronizeExpansion(event.getPath(), false);
            }
        };
        
        //Listener ScrollBar
        JScrollBar scrollLeft = scrollPanelLeft.getVerticalScrollBar();
        JScrollBar scrollRight = scrollPanelRight.getVerticalScrollBar();
        AdjustmentListener listenerScrollLeft = new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				scrollRight.setValue(scrollLeft.getValue());
			}
        };
        AdjustmentListener listenerScrollRight = new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
		        scrollLeft.setValue(scrollRight.getValue());
			}
        };
        
        //Synchroniser par défault
        treeLeft.addTreeExpansionListener(listenerTreeLeft);
        treeRight.addTreeExpansionListener(listenerTreeRight);
        scrollLeft.addAdjustmentListener(listenerScrollLeft);
        scrollRight.addAdjustmentListener(listenerScrollRight);
        this.synchronizeBtn.setSelected(true);
        
        this.synchronizeBtn.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(!synchronizeBtn.isSelected()) {
					synchronizeBtn.setSelected(true);
		            treeLeft.addTreeExpansionListener(listenerTreeLeft);
		            treeRight.addTreeExpansionListener(listenerTreeRight);
		            
		            scrollLeft.addAdjustmentListener(listenerScrollLeft);
		            scrollRight.addAdjustmentListener(listenerScrollRight);
				}
				else {
					synchronizeBtn.setSelected(false);
					treeLeft.removeTreeExpansionListener(listenerTreeLeft);
					treeRight.removeTreeExpansionListener(listenerTreeRight);
					
		            scrollLeft.removeAdjustmentListener(listenerScrollLeft);
		            scrollRight.removeAdjustmentListener(listenerScrollRight);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
        	
        });
    }

    private void initDiffOnly() {
    	this.diffOnlyBtn.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if(diffOnlyBtn.isSelected()) {
					diffOnlyBtn.setSelected(false);
					treeLeft.update(controller.getVPIData(PositionFile.LEFT), diffOnlyBtn.isSelected());
				}
				else {
					diffOnlyBtn.setSelected(true);
					treeLeft.update(controller.getVPIData(PositionFile.LEFT), diffOnlyBtn.isSelected());
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
    		
    	});
    }
}
