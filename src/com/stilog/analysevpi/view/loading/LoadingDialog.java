package com.stilog.analysevpi.view.loading;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class LoadingDialog extends JDialog {

    private JLabel messageLabel;

    public LoadingDialog(JFrame parent, String message) {
        super(parent, true); // modal

        setUndecorated(true); // optionnel (fenêtre simple)
        setLayout(new BorderLayout());

        messageLabel = new JLabel(message, JLabel.CENTER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);

        add(messageLabel, BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);

        setSize(260, 100);
        setLocationRelativeTo(parent);
    }

    public void setMessage(String msg) {
        messageLabel.setText(msg);
    }
}