package com.stilog.analysevpi.view.filter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.stilog.vpimodel.objects.filter.FilterGroupNode;

/**
 * Dialog d'affichage d'un seul filtre VP (modale).
 * Le rendu est délégué à FilterPanelBuilder.
 */
public class FilterDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public FilterDialog(Component parent, String filterName, FilterGroupNode root) {
        super(javax.swing.SwingUtilities.getWindowAncestor(parent),
              "Filtre : " + filterName,
              ModalityType.APPLICATION_MODAL);

        setSize(580, 520);
        setLocationRelativeTo(parent);
        setResizable(true);

        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(FilterPanelBuilder.BG_PANEL);
        outer.setBorder(new EmptyBorder(4, 0, 4, 0));

        outer.add(FilterPanelBuilder.buildFilterPanel(filterName, root), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        bottom.setBackground(FilterPanelBuilder.BG_PANEL);
        JButton closeBtn = new JButton("Fermer");
        closeBtn.putClientProperty("JButton.buttonType", "roundRect");
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);
        outer.add(bottom, BorderLayout.SOUTH);

        setContentPane(outer);
    }
}
