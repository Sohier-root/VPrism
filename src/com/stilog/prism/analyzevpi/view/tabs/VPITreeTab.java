package com.stilog.prism.analyzevpi.view.tabs;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

import com.stilog.prism.analyzevpi.controller.SingleVPIController;
import com.stilog.prism.comparevpi.view.filter.FilterDialog;
import com.stilog.prism.comparevpi.view.loading.LoadingWindow;
import com.stilog.prism.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.prism.vpimodel.utils.FilterParser;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.stilog.prism.comparevpi.view.tree.CustomTreeCellRenderer;
import com.stilog.prism.view.ThemeManager;
import com.stilog.prism.vpimodel.objects.Attribute;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.VPIDatas;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;

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

        // Ouvrir la dialog de filtre au double-clic sur une entité ou un Parameters de type filtre
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null)
                        tryOpenFilterDialog((DefaultMutableTreeNode) path.getLastPathComponent());
                }
            }
        });

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

    /**
     * Double-clic sur un nœud : ouvre FilterDialog si le nœud (ou son parent Entity)
     * appartient à un FileDatas de type filtre.
     * Reprend la même logique que VPITree.tryOpenFilterDialog() du module 1.
     */
    private void tryOpenFilterDialog(DefaultMutableTreeNode node) {
        Object obj = node.getUserObject();

        // Accepter Entity ou Parameters
        if (obj instanceof Entity) {
            // Descendre sur le premier enfant Parameters
            if (node.getChildCount() == 0) return;
            node = (DefaultMutableTreeNode) node.getChildAt(0);
            obj  = node.getUserObject();
        }
        if (!(obj instanceof Parameters)) return;

        Parameters param = (Parameters) obj;

        // Remonter pour trouver le FileDatas parent et vérifier que c'est un filtre
        javax.swing.tree.TreeNode current = node.getParent();
        FileDatas parentFd = null;
        while (current instanceof DefaultMutableTreeNode dmtn) {
            Object uObj = dmtn.getUserObject();
            if (uObj instanceof FileDatas fd) { parentFd = fd; break; }
            current = current.getParent();
        }
        if (parentFd == null) return;

        String fdName = parentFd.getName();
        boolean isFilter = fdName.equals(VPIConstants.NAME_TREE_RESOURCESFILTER)
                        || fdName.equals(VPIConstants.NAME_TREE_EVENTSFILTER);
        if (!isFilter) return;

        boolean isEvent = fdName.equals(VPIConstants.NAME_TREE_EVENTSFILTER);

        String condXml = param.getAttributeValue(VPIConstants.PARAMETER_CONDITIONS);
        if (condXml == null || condXml.isBlank()) return;

        FilterGroupNode root = FilterParser.parse(condXml, isEvent);
        new FilterDialog(panel, param.getName(), root).setVisible(true);
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
