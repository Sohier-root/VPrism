package com.stilog.prism.comparevpi.view.documentation;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DocumentationWindow extends JFrame {
    
    private JEditorPane editorPane;
    private String cssContent;
    private static final String BASE_RESOURCE_PATH = "/com/stilog/analysevpi/view/documentation/web/";
    
    public DocumentationWindow() {
        super("Documentation - VP Comparator");
        setSize(950, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Charger le CSS une seule fois
        try {
            cssContent = loadCSSFromResources();
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du CSS: " + e.getMessage());
            cssContent = ""; // CSS par défaut vide
        }
        
        // Créer le panneau de navigation
        JPanel navPanel = createNavigationPanel();
        
        // Créer le panneau de contenu
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html; charset=UTF-8");
        
        // Gérer les liens cliqués
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String href = e.getDescription();
                if (href.startsWith("#")) {
                    loadPage(href.substring(1));
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(editorPane);
        
        // Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(navPanel);
        splitPane.setRightComponent(scrollPane);
        splitPane.setDividerLocation(220);
        splitPane.setResizeWeight(0.0);
        
        getContentPane().add(splitPane);
        
        // Charger la page d'accueil
        loadPage("introduction");
    }
    
    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(245, 245, 245));
        
        JLabel title = new JLabel("📚 Sommaire");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        String[] menuItems = {
            "Introduction",
            "Interface",
            "Comparer des fichiers"
        };
        
        String[] menuKeys = {
            "introduction",
            "interface",
            "comparer",
        };
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String item : menuItems) {
            listModel.addElement(item);
        }
        
        JList<String> navList = new JList<>(listModel);
        navList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navList.setSelectedIndex(0);
        navList.setFont(new Font("Arial", Font.PLAIN, 13));
        navList.setFixedCellHeight(35);
        
        navList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = navList.getSelectedIndex();
                if (index >= 0) {
                    loadPage(menuKeys[index]);
                }
            }
        });
        
        JScrollPane scrollNav = new JScrollPane(navList);
        scrollNav.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollNav, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Charge une page HTML depuis les ressources
     */
    private void loadPage(String pageKey) {
        try {
            String htmlContent = loadHTMLFromResources(pageKey);
            // Traiter les chemins des images avant d'injecter le CSS
            String processedHTML = processImagePaths(htmlContent);
            String finalHTML = injectCSS(processedHTML);
            
            editorPane.setText(finalHTML);
            editorPane.setCaretPosition(0);
        } catch (IOException e) {
            editorPane.setText(
                "<html><body style='font-family: Arial; padding: 20px;'>" +
                "<h1>Erreur</h1>" +
                "<p>Impossible de charger la page : " + pageKey + "</p>" +
                "<p>Erreur : " + e.getMessage() + "</p>" +
                "</body></html>"
            );
        }
    }
    
    /**
     * Charge le contenu HTML depuis le dossier resources/documentation
     */
    private String loadHTMLFromResources(String pageName) throws IOException {
        String resourcePath = BASE_RESOURCE_PATH + pageName + ".html";
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    getClass().getResourceAsStream(resourcePath),
                    StandardCharsets.UTF_8
                )
            )) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Charge le CSS depuis les ressources
     */
    private String loadCSSFromResources() throws IOException {
        String resourcePath = BASE_RESOURCE_PATH + "css/style.css";
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    getClass().getResourceAsStream(resourcePath),
                    StandardCharsets.UTF_8
                )
            )) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Convertit les chemins relatifs des images en URLs absolues
     * Transforme: src="resources/image.png" en src="file:///chemin/complet/vers/image.png"
     */
    private String processImagePaths(String htmlContent) {
        // Obtenir l'URL du dossier resources
        URL resourcesUrl = getClass().getResource(BASE_RESOURCE_PATH + "resources/");
        
        if (resourcesUrl != null) {
            String resourcesPath = resourcesUrl.toString();
            // Assurer que le chemin se termine par /
            if (!resourcesPath.endsWith("/")) {
                resourcesPath += "/";
            }
            
            // Remplacer tous les chemins relatifs par le chemin absolu
            return htmlContent.replace("src=\"resources/", "src=\"" + resourcesPath);
        } else {
            System.err.println("Impossible de trouver le dossier resources/");
            return htmlContent;
        }
    }
    
    /**
     * Injecte le CSS dans le HTML
     */
    private String injectCSS(String htmlContent) {
        // Si le HTML contient déjà une balise <head>, injecter le style dedans
        if (htmlContent.contains("<head>")) {
            return htmlContent.replace("<head>", "<head>\n<style>\n" + cssContent + "\n</style>");
        } else if (htmlContent.contains("<html>")) {
            // Sinon, créer une balise head
            return htmlContent.replace("<html>", "<html>\n<head>\n<style>\n" + cssContent + "\n</style>\n</head>");
        } else {
            // En dernier recours, wrapper le tout
            return "<html><head><style>" + cssContent + "</style></head><body>" + htmlContent + "</body></html>";
        }
    }
}