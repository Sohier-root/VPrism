package com.stilog.documentation.view;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.stilog.analysevpi.view.loading.LoadingWindow;
import com.stilog.documentation.controller.DocumentationModelController;
import com.stilog.documentation.controller.DocumentationTemplateController;
import com.stilog.documentation.model.DocumentationModel;
import com.stilog.documentation.model.dto.DocumentProcessingRequest;
import com.stilog.documentation.view.tree.CheckBoxTree;
import com.stilog.documentation.view.tree.CheckBoxTreeNode;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;

public class DocumentationMainPage extends JPanel{

	private DocumentationTemplateController templateController;
	private DocumentationModelController modelController;
	JFrame parentFrame;
	
	private JTextField fileField;
	private JButton browseBtn;
	private File vpiFile;
	private CheckBoxTree dataTree;
	
	public DocumentationMainPage(JFrame parentFrame) {
		this.parentFrame = parentFrame;
		this.setLayout(new BorderLayout());
		
		DocumentationModel model = new DocumentationModel();
		modelController = new DocumentationModelController(model);
		templateController = new DocumentationTemplateController();
		
		DocumentProcessingRequest request = new DocumentProcessingRequest("E:\\result.docx");
		//templateController.processDocuments(request);
		
		JPanel principlePanel = createPanel("VPI/VPS");
		
		this.add(principlePanel, BorderLayout.CENTER);
	}
	
	public JPanel createPanel(String title) {
		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createTitledBorder(title));
		
		// Top: file chooser controls
		JPanel top = new JPanel(new BorderLayout(6, 6));
		JTextField fileField = new JTextField();
		fileField.setEditable(false);
		JButton browseBtn = new JButton("Importer...");
		browseBtn.putClientProperty("JButton.buttonType", "roundRect");

		top.add(fileField, BorderLayout.CENTER);
		top.add(browseBtn, BorderLayout.EAST);
		
		panel.add(top, BorderLayout.NORTH);
		
        // Center: Arbre avec cases à cocher
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // Créer l'arbre de données
        CheckBoxTreeNode root = new CheckBoxTreeNode("null");
        dataTree = new CheckBoxTree(root);
        
        JScrollPane scrollPane = new JScrollPane(dataTree);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        // Bottom: Bouton de génération
        JPanel bottom = new JPanel();
        JButton generateBtn = new JButton("Générer la documentation");
        generateBtn.putClientProperty("JButton.buttonType", "roundRect");
        generateBtn.addActionListener(e ->{
        	generateDocumentation();
        });
        bottom.add(generateBtn);
        
        panel.add(bottom, BorderLayout.SOUTH);
        
		this.fileField = fileField;
		this.browseBtn = browseBtn;
		
		// Browse action
		browseBtn.addActionListener(e -> {
			chooseVPIFile();
			LoadingWindow.run(parentFrame, () -> {
				modelController.handleFile(vpiFile);
				dataTree.update(modelController.getVPIData());
			});
		});
				
		return panel;
	}
    
    /**
     * Génère la documentation avec les éléments cochés
     */
    private void generateDocumentation() {
        List<CheckBoxTreeNode> selectedNodes = dataTree.getSelectedNodes();
        
        if (selectedNodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Veuillez sélectionner au moins un élément à inclure.",
                "Aucune sélection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Map<Entity, List<Parameters>> paramToInsert = new HashMap<>();
        Entity lastEntity = null;
        for (CheckBoxTreeNode node : selectedNodes) {
        	Object obj = node.getUserObject();
        	
        	if(obj instanceof Entity) {
        		paramToInsert.put(((Entity) obj), new ArrayList<>());
        		lastEntity = (Entity) obj;
        	}
        	if(obj instanceof Parameters) {
        		paramToInsert.get(lastEntity).add((Parameters) obj);
        	}
        	
            System.out.println("- " + node.getUserObject());
        }
        
        // TODO: Générer la documentation
        LoadingWindow.run(parentFrame, () -> {
            DocumentProcessingRequest request = new DocumentProcessingRequest("E:\\result.docx");
            // Ajouter les nœuds sélectionnés à la requête
            templateController.processDocuments(request, paramToInsert);
        });
        
        JOptionPane.showMessageDialog(this,
            "Documentation générée avec succès !",
            "Succès",
            JOptionPane.INFORMATION_MESSAGE);
    }
	
	private void chooseVPIFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// Optional: set filters
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Fichiers VPI/VPS", "vpi", "vps");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(vpiFile);
		int res = chooser.showOpenDialog(this);
		if (res == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			vpiFile = f;
			fileField.setText(f.getAbsolutePath());
		}
	}
}
