package com.stilog.view;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import com.stilog.analysevpi.controller.ComparisonController;
import com.stilog.analysevpi.model.ComparisonModel;
import com.stilog.analysevpi.utils.SystemInfo;
import com.stilog.analysevpi.view.ComparisonMainPage;

import com.stilog.documentation.view.DocumentationMainPage;

public class MainFrame extends JFrame{

	private SystemInfo infos = SystemInfo.getInstance();
	
    private JPanel contentPanel;
    private JMenuBar menuBar;
    
    public MainFrame() {
		super("VP Comparator");
		this.setTitle(this.getTitle() + " (" + infos.getVersion() + ")");
		
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Initialiser le panneau de contenu
        contentPanel = new JPanel(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
        
        // Créer la barre de menu
        createMenuBar();
        
        //Page par default
        switchToModule(createComparisonPage());
        
        setVisible(true);
    }
    
    private void createMenuBar() {
        menuBar = new JMenuBar();
        
        // Menu Fichier
        JMenu fileMenu = new JMenu("Fichier");
        JMenuItem exitItem = new JMenuItem("Quitter");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // Menu Modules
        JMenu modulesMenu = new JMenu("Modules");
        
        JMenuItem module1Item = new JMenuItem("Module 1");
        module1Item.addActionListener(e -> {
        	switchToModule(createComparisonPage());
        });
        
        JMenuItem module2Item = new JMenuItem("Module 2");
        module2Item.addActionListener(e -> {
        	switchToModule(createDocumentationPage());
        });
        
        modulesMenu.add(module1Item);
        modulesMenu.add(module2Item);
        
        menuBar.add(fileMenu);
        menuBar.add(modulesMenu);
        
        setJMenuBar(menuBar);
    }
    
    /**
     * Méthode pour changer le contenu de la fenêtre
     */
    public void switchToModule(JPanel modulePanel) {
        contentPanel.removeAll();
        contentPanel.add(modulePanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    public ComparisonMainPage createComparisonPage() {
    	ComparisonModel model = new ComparisonModel();
    	ComparisonController controller = new ComparisonController(model);
    	return new ComparisonMainPage(controller, this);
    }
    
    public DocumentationMainPage createDocumentationPage() {
    	return new DocumentationMainPage(this);
    }
}
