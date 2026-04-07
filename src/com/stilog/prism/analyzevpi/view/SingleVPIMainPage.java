package com.stilog.prism.analyzevpi.view;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.stilog.prism.analyzevpi.controller.SingleVPIController;
import com.stilog.prism.analyzevpi.view.tabs.AbstractVPITab;
import com.stilog.prism.analyzevpi.view.tabs.VPIDependencyDiagramTab;
import com.stilog.prism.analyzevpi.view.tabs.VPITreeTab;

/**
 * Panneau principal du Module 2 : manipulation d'un VPI/VPS unique.
 * <p>
 * L'architecture repose sur un {@link JTabbedPane} alimenté par une liste
 * d'implémentations de {@link AbstractVPITab}.
 * <p>
 * <b>Pour ajouter un nouvel onglet</b> (ex. sérialisation JSON, export CSV…) :
 * <ol>
 *   <li>Créer une classe implémentant {@code AbstractVPITab}</li>
 *   <li>L'ajouter dans {@link #buildTabs()} — une seule ligne</li>
 * </ol>
 * Les nouvelles données sont propagées automatiquement à tous les onglets
 * via le mécanisme de listener de {@link VPITreeTab}.
 */
public class SingleVPIMainPage extends JPanel {

    private final SingleVPIController controller;
    private final JFrame parentFrame;

    /** Liste ordonnée de tous les onglets enregistrés. */
    private final List<AbstractVPITab> tabs = new ArrayList<>();

    private JTabbedPane tabbedPane;

    public SingleVPIMainPage(SingleVPIController controller, JFrame parentFrame) {
        this.controller = controller;
        this.parentFrame = parentFrame;

        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        buildTabs();

        add(tabbedPane, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    // Construction des onglets — point d'extension central
    // -------------------------------------------------------------------------

    /**
     * Déclare tous les onglets du module.
     * Ajouter une ligne ici suffit pour intégrer un nouvel onglet.
     */
    private void buildTabs() {
        // Onglet 1 : arbre VPI/VPS (import inclus)
        VPITreeTab treeTab = new VPITreeTab(controller, parentFrame);

        // Onglet 2 : diagramme de dépendances
        VPIDependencyDiagramTab diagramTab = new VPIDependencyDiagramTab();

        // Câblage : quand un fichier est chargé dans l'onglet arbre,
        // tous les autres onglets sont notifiés.
        treeTab.addOnLoadListener(data -> {
            for (AbstractVPITab tab : tabs) {
                if (tab != treeTab) {
                    tab.onDataLoaded(data);
                }
            }
        });

        // === Enregistrement ===
        // Ajoutez simplement vos nouveaux onglets ici :
        registerTab(treeTab);
        registerTab(diagramTab);
        // registerTab(new JsonExportTab(controller));
        // registerTab(new CsvExportTab(controller));
    }

    // -------------------------------------------------------------------------
    // Plomberie
    // -------------------------------------------------------------------------

    private void registerTab(AbstractVPITab tab) {
        tabs.add(tab);
        tabbedPane.addTab(tab.getTabTitle(), tab.getPanel());
    }
}
