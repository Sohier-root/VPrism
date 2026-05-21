package com.stilog.prism.comparevpi.view.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import com.stilog.prism.vpimodel.objects.filter.FilterGroupNode;

/**
 * Dialog de comparaison côte à côte de deux filtres VP.
 * Gauche = Référence, Droite = Testé.
 * Non modale : peut rester ouverte pendant l'utilisation de l'application.
 */
public class FilterCompareDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final Color BG          = FilterPanelBuilder.BG_PANEL;
    private static final Color LABEL_REF   = new Color(30,  90, 190);  // bleu — référence
    private static final Color LABEL_TEST  = new Color(150, 50, 170);  // violet — testé

    /**
     * @param parent      composant parent pour le positionnement
     * @param filterName  nom du filtre (commun aux deux côtés)
     * @param refGroup    conditions du VPI de référence (null si absent)
     * @param testGroup   conditions du VPI testé (null si absent)
     */
    public FilterCompareDialog(Component parent, String filterName,
                               FilterGroupNode refGroup, FilterGroupNode testGroup) {
        super(javax.swing.SwingUtilities.getWindowAncestor(parent),
              "Comparaison : " + filterName,
              ModalityType.MODELESS);   // non bloquant

        setSize(1100, 580);
        setLocationRelativeTo(parent);
        setResizable(true);

        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(BG);

        // En-tête avec le nom du filtre
        JPanel header = buildHeader(filterName);
        outer.add(header, BorderLayout.NORTH);

        // Panneau gauche — Référence
        JPanel leftPanel  = buildSidePanel("Référence", LABEL_REF,  refGroup);
        // Panneau droit — Testé
        JPanel rightPanel = buildSidePanel("Testé",     LABEL_TEST, testGroup);

        // Split pane au centre
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setResizeWeight(0.5);
        split.setDividerSize(6);
        split.setBorder(null);
        outer.add(split, BorderLayout.CENTER);

        // Bouton fermer
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        bottom.setBackground(BG);
        JButton closeBtn = new JButton("Fermer");
        closeBtn.putClientProperty("JButton.buttonType", "roundRect");
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);
        outer.add(bottom, BorderLayout.SOUTH);

        setContentPane(outer);
    }

    /** Barre de titre avec le nom du filtre centré */
    private JPanel buildHeader(String filterName) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        header.setBackground(BG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 224, 230)));

        JLabel nameLabel = new JLabel(filterName);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        nameLabel.setForeground(FilterPanelBuilder.ATTR_FG);
        header.add(nameLabel);

        return header;
    }

    /**
     * Construit un panneau latéral avec :
     *  - un bandeau coloré indiquant "Référence" ou "Testé"
     *  - le rendu du filtre (ou un message si absent)
     */
    private JPanel buildSidePanel(String sideLabel, Color labelColor, FilterGroupNode group) {
        JPanel side = new JPanel(new BorderLayout(0, 0));
        side.setBackground(BG);

        // Bandeau de côté
        JPanel labelBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        labelBar.setBackground(FilterPanelBuilder.blend(labelColor, Color.WHITE, 0.92f));
        labelBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                FilterPanelBuilder.blend(labelColor, Color.WHITE, 0.70f)));

        JLabel label = new JLabel(sideLabel);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        label.setForeground(labelColor);
        labelBar.add(label);

        side.add(labelBar, BorderLayout.NORTH);

        // Contenu : passe null si absent → FilterPanelBuilder affiche "Filtre absent"
        side.add(FilterPanelBuilder.buildFilterPanel(null, group), BorderLayout.CENTER);

        return side;
    }
}
