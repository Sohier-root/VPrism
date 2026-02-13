package com.stilog;

import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.stilog.analysevpi.controller.ComparisonController;
import com.stilog.analysevpi.model.ComparisonModel;
import com.stilog.analysevpi.view.ComparisonMainPage;
import com.stilog.view.MainFrame;

public class Main {
    public static void main(String[] args) {
        
    	configureInterface();
        
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
    
    private static void configureInterface() {
    	
    	//Design interface
    	FlatLightLaf.setup();
    	UIManager.getLookAndFeelDefaults().put("Tree.showDefaultIcons", true);
    	UIManager.put("Tree.leafIcon",   UIManager.getIcon("FileView.fileIcon"));
    	UIManager.put("Tree.closedIcon", UIManager.getIcon("FileChooser.listViewIcon"));
    	UIManager.put("Tree.openIcon",   UIManager.getIcon("FileChooser.detailsViewIcon"));
    	
    	//ToolTip Delay
    	ToolTipManager.sharedInstance().setInitialDelay(250);
    }
}
