package com.stilog.prism.analyzevpi.view.tabs;

import javax.swing.JPanel;

import com.stilog.prism.vpimodel.objects.VPIDatas;

/**
 * Interface de base pour tous les onglets du Module 2.
 * <p>
 * Pour ajouter un nouvel onglet de sérialisation ou d'analyse du VPI/VPS,
 * il suffit :
 * <ol>
 *   <li>Créer une classe implémentant {@code AbstractVPITab}</li>
 *   <li>L'enregistrer dans {@code SingleVPIMainPage#buildTabs()}</li>
 * </ol>
 * Aucune autre modification n'est nécessaire.
 */
public interface AbstractVPITab {

    /**
     * @return le libellé de l'onglet affiché dans le JTabbedPane.
     */
    String getTabTitle();

    /**
     * @return le panel Swing à afficher dans l'onglet.
     */
    JPanel getPanel();

    /**
     * Appelé chaque fois que le VPIDatas chargé change (nouveau fichier importé).
     * L'onglet doit rafraîchir son contenu en conséquence.
     *
     * @param data les nouvelles données VPI/VPS, ou {@code null} si aucune donnée disponible.
     */
    void onDataLoaded(VPIDatas data);
}
