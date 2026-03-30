package com.stilog.analyzemodule.view.tabs;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

import com.stilog.analyzemodule.controller.SingleVPIController;
import com.stilog.analysevpi.view.loading.LoadingWindow;
import com.stilog.analysevpi.view.tree.CustomTreeCellRenderer;
import com.stilog.view.ThemeManager;
import com.stilog.vpimodel.objects.Attribute;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.VPIDatas;
import com.stilog.vpimodel.vpsettings.FileDatas;

public class VPITreeTab implements AbstractVPITab {

    private static final String TAB_TITLE = "Arbre VPI/VPS";

    private final ThemeManager theme = ThemeManager.getInstance();
    private final SingleVPIController controller;
    private final JFrame parentFrame;

    private final JPanel panel;
    private final JTextField fileField;
    private final JTree tree;
    private final JScrollPane scrollPane;

    private final List<Consumer<VPIDatas>> onLoadListeners = new ArrayList<>();

    public VPITreeTab(SingleVPIController controller, JFrame parentFrame) {
        this.controller = controller;
        this.parentFrame = parentFrame;

        panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(theme.bg());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Card
        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(theme.cardBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(theme.cardBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // Top section
        JPanel topSection = new JPanel(new BorderLayout(0, 10));
        topSection.setOpaque(false);

        Color badgeColor = theme.badgeTest();
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        JLabel badge = new JLabel("Fichier VPI/VPS") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setForeground(badgeColor);
        badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        badge.setOpaque(false);
        titleRow.add(badge);

        JPanel fileRow = new JPanel(new BorderLayout(8, 0));
        fileRow.setOpaque(false);
        fileField = createStyledTextField("Aucun fichier sélectionné…");
        JButton browseBtn = createPrimaryButton("Importer…", MaterialDesign.MDI_FOLDER_DOWNLOAD);
        fileRow.add(fileField, BorderLayout.CENTER);
        fileRow.add(browseBtn, BorderLayout.EAST);

        topSection.add(titleRow, BorderLayout.NORTH);
        topSection.add(fileRow, BorderLayout.CENTER);

        // Tree
        tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("VPI Datas")));
        tree.setCellRenderer(new CustomTreeCellRenderer());
        tree.setBackground(theme.cardBg());
        scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createLineBorder(theme.cardBorder(), 1));
        scrollPane.getViewport().setBackground(theme.cardBg());

        card.add(topSection, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);

        // Réappliquer les couleurs du tree après chaque changement de thème
        // (updateComponentTreeUI réinitialise background aux valeurs LAF par défaut)
        theme.addChangeListener(() -> {
            tree.setBackground(theme.cardBg());
            scrollPane.getViewport().setBackground(theme.cardBg());
            scrollPane.setBorder(BorderFactory.createLineBorder(theme.cardBorder(), 1));
            tree.repaint();
        });

        browseBtn.addActionListener(e -> {
            File chosen = chooseFile();
            if (chosen != null) {
                fileField.setText(chosen.getAbsolutePath());
                LoadingWindow.run(parentFrame, () -> {
                    VPIDatas data = controller.handleFile(chosen);
                    SwingUtilities.invokeLater(() -> {
                        refreshTree(data);
                        notifyListeners(data);
                    });
                });
            }
        });
    }

    @Override public String getTabTitle() { return TAB_TITLE; }
    @Override public JPanel getPanel()    { return panel; }
    @Override public void onDataLoaded(VPIDatas data) { refreshTree(data); }

    public void addOnLoadListener(Consumer<VPIDatas> listener) { onLoadListeners.add(listener); }

    private File chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers VPI/VPS", "vpi", "vps"));
        return (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) ? chooser.getSelectedFile() : null;
    }

    private void refreshTree(VPIDatas data) {
        if (data == null) return;
        tree.setCellRenderer(new CustomTreeCellRenderer());
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(data);
        for (FileDatas fd : data.getFilesDatas()) {
            if (fd == null) continue;
            fd.sort();
            root.add(buildFileNode(fd));
        }
        tree.setModel(new DefaultTreeModel(root));
    }

    private DefaultMutableTreeNode buildFileNode(FileDatas fileDatas) {
        DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(fileDatas);
        for (Entity entity : fileDatas.getEntities()) {
            DefaultMutableTreeNode entityNode = new DefaultMutableTreeNode(entity);
            for (Parameters param : entity.getParameters()) {
                DefaultMutableTreeNode paramNode = new DefaultMutableTreeNode(param);
                for (Attribute attr : param.getAttributes()) paramNode.add(new DefaultMutableTreeNode(attr));
                entityNode.add(paramNode);
            }
            fileNode.add(entityNode);
        }
        return fileNode;
    }

    private void notifyListeners(VPIDatas data) {
        for (Consumer<VPIDatas> l : onLoadListeners) l.accept(data);
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(theme.fieldBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(theme.fieldBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setEditable(false);
        tf.setOpaque(false);
        tf.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        tf.setText(placeholder);
        tf.setForeground(theme.textDim());
        tf.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return tf;
    }

    private JButton createPrimaryButton(String label, MaterialDesign icon) {
        JButton btn = new JButton(label, FontIcon.of(icon, 16, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = isEnabled()
                    ? (getModel().isRollover() ? theme.accentHover() : theme.accent())
                    : theme.cardBorder();
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setIconTextGap(6);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
