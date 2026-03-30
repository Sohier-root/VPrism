package com.stilog;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.stilog.view.MainFrame;
import com.stilog.view.ThemeManager;

public class Main {
    public static void main(String[] args) {
        configureInterface();
        SwingUtilities.invokeLater(() -> new MainFrame());
    }

    private static void configureInterface() {
        // Le ThemeManager lit la préférence sauvegardée et applique le bon LAF
        ThemeManager.getInstance().applyLafOnStartup();

        // Icônes de l'arbre
        UIManager.getLookAndFeelDefaults().put("Tree.showDefaultIcons", true);
        UIManager.put("Tree.leafIcon",   UIManager.getIcon("FileView.fileIcon"));
        UIManager.put("Tree.closedIcon", UIManager.getIcon("FileChooser.listViewIcon"));
        UIManager.put("Tree.openIcon",   UIManager.getIcon("FileChooser.detailsViewIcon"));

        // Tooltip
        ToolTipManager.sharedInstance().setInitialDelay(250);
        ToolTipManager.sharedInstance().setDismissDelay(8000);
    }
}
