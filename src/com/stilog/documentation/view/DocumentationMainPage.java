package com.stilog.documentation.view;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.stilog.analysevpi.controller.ComparisonController;

public class DocumentationMainPage extends JPanel{

	JFrame parentFrame;
	
	public DocumentationMainPage(JFrame parentFrame) {
		this.parentFrame = parentFrame;
		this.setLayout(new BorderLayout());
	}
}
