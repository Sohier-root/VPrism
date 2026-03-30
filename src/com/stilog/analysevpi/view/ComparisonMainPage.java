package com.stilog.analysevpi.view;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

import com.stilog.analysevpi.controller.ComparisonController;
import com.stilog.analysevpi.utils.SystemInfo;
import com.stilog.analysevpi.view.documentation.DocumentationWindow;
import com.stilog.analysevpi.view.loading.LoadingIcon;
import com.stilog.analysevpi.view.loading.LoadingWindow;
import com.stilog.analysevpi.view.object.VButton;
import com.stilog.analysevpi.view.tree.VPITree;
import com.stilog.view.ThemeManager;
import com.stilog.vpimodel.objects.TypeFile;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public class ComparisonMainPage extends JPanel {

    private static final String TOOLTIP_SYNCHRONIZE     = "Synchroniser les affichages";
    private static final String TOOLTIP_DESYNCHRONIZE   = "Désynchroniser les affichages";
    private static final String TOOLTIP_DISPLAY_DIFFONLY = "Afficher seulement les différences";
    private static final String TOOLTIP_DISPLAY_ALL     = "Tout afficher";
    private static final String TOOLTIP_GENERATE_VPI    = "Générer un nouveau VPI/VPS";
    private static final String TOOLTIP_REVERSE         = "Intervertir les affichages";

    private final ThemeManager theme = ThemeManager.getInstance();

    private ComparisonController controller;
    private SystemInfo infos = SystemInfo.getInstance();
    private JFrame parentFrame;

    private JTextField leftFileField;
    private JButton browseBtnLeft;
    private VPITree treeLeft;
    private File leftSelectedFile;
    private JScrollPane scrollPanelLeft;

    private JTextField rightFileField;
    private JButton browseBtnRight;
    private VPITree treeRight;
    private File rightSelectedFile;
    private JScrollPane scrollPanelRight;

    private VButton compareBtn;
    private JToggleButton synchronizeBtn;
    private JToggleButton diffOnlyBtn;
    private JButton generateFilesBtn;
    private JButton helpBtn;
    private JButton reverseDatasBtn;

    private FontIcon generateFilesIconNormal;
    private LoadingIcon generateFilesIconLoading;

    public ComparisonMainPage(ComparisonController controller, JFrame parentFrame) {
        this.controller = controller;
        this.parentFrame = parentFrame;

        setBackground(theme.bg());
        setLayout(new BorderLayout(0, 0));

        add(createToolbarPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = createStyledSplitPane();
        splitPane.setLeftComponent(createZonePanel("Référence", true));
        splitPane.setRightComponent(createZonePanel("Testé", false));
        add(splitPane, BorderLayout.CENTER);

        add(createBottomPanel(), BorderLayout.SOUTH);

        initSynchro();
        initializeDiffOnlyToggle();
        initializeGenerateFiles();
        initializeReverseDatas();

        // Mettre à jour les icônes toolbar au changement de thème
        theme.addChangeListener(this::refreshToolbarIcons);
    }

    private void refreshToolbarIcons() {
        helpBtn.setIcon(FontIcon.of(MaterialDesign.MDI_HELP_CIRCLE, 18, theme.text()));
        reverseDatasBtn.setIcon(FontIcon.of(MaterialDesign.MDI_SWAP_HORIZONTAL, 18, theme.text()));
        generateFilesIconNormal = FontIcon.of(MaterialDesign.MDI_FILE_CHECK, 18, new Color(130, 220, 160));
        // Ne remplacer l'icône de generateFilesBtn que s'il n'est pas en train de charger
        if (generateFilesBtn.getIcon() != generateFilesIconLoading) {
            generateFilesBtn.setIcon(generateFilesIconNormal);
        }
        // synchronizeBtn : l'icône dépend de l'état sélectionné
        MaterialDesign syncIcon = synchronizeBtn.isSelected()
            ? MaterialDesign.MDI_SYNC : MaterialDesign.MDI_SYNC_OFF;
        synchronizeBtn.setIcon(FontIcon.of(syncIcon, 18, theme.text()));
        // diffOnlyBtn : icône fixe, couleur selon activation
        diffOnlyBtn.setIcon(FontIcon.of(MaterialDesign.MDI_VECTOR_DIFFERENCE, 18, theme.text()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Zone panels
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createZonePanel(String title, boolean isLeft) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
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
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        Color badgeColor = isLeft ? theme.badgeRef() : theme.badgeTest();

        // Badge title
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel badge = new JLabel(title) {
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

        // File row
        JPanel fileRow = new JPanel(new BorderLayout(6, 0));
        fileRow.setOpaque(false);
        fileRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JTextField fileField = createStyledTextField();
        JButton browseBtn = createPrimaryButton("Importer…", MaterialDesign.MDI_FOLDER_LOCK_OPEN);
        browseBtn.setEnabled(isLeft);
        fileRow.add(fileField, BorderLayout.CENTER);
        fileRow.add(browseBtn, BorderLayout.EAST);

        VPITree tree = new VPITree(this.controller, isLeft);
        JScrollPane scroll = createStyledScrollPane(tree);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleRow, BorderLayout.NORTH);
        top.add(fileRow, BorderLayout.CENTER);

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);

        if (isLeft) {
            this.leftFileField = fileField; this.treeLeft = tree;
            this.browseBtnLeft = browseBtn; this.scrollPanelLeft = scroll;
        } else {
            this.rightFileField = fileField; this.treeRight = tree;
            this.treeRight.setOtherTree(this.treeLeft);
            this.treeLeft.setOtherTree(this.treeRight);
            this.browseBtnRight = browseBtn; this.scrollPanelRight = scroll;
        }

        browseBtn.addActionListener(e -> {
            chooseVPIFile(isLeft);
            LoadingWindow.run(parentFrame, () -> {
                processFile(isLeft);
                if (isLeft) this.browseBtnRight.setEnabled(true);
            });
        });

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        outer.add(wrapper, BorderLayout.CENTER);
        return outer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toolbar
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createToolbarPanel() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(theme.headerBg());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(theme.separator());
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);

        this.helpBtn = createToolbarButton(MaterialDesign.MDI_HELP_CIRCLE, "Aide et documentation");
        this.helpBtn.addActionListener(e -> openDocumentation());

        this.synchronizeBtn = createToolbarToggleButton(MaterialDesign.MDI_SYNC, TOOLTIP_DESYNCHRONIZE);
        this.diffOnlyBtn    = createToolbarToggleButton(MaterialDesign.MDI_VECTOR_DIFFERENCE, TOOLTIP_DISPLAY_DIFFONLY);
        this.diffOnlyBtn.setEnabled(false);

        this.generateFilesIconNormal  = FontIcon.of(MaterialDesign.MDI_FILE_CHECK, 18, new Color(130, 220, 160));
        this.generateFilesIconLoading = new LoadingIcon();
        this.generateFilesBtn = createToolbarButton(MaterialDesign.MDI_FILE_CHECK, TOOLTIP_GENERATE_VPI);
        this.generateFilesBtn.setIcon(generateFilesIconNormal);
        this.generateFilesBtn.setEnabled(false);

        this.reverseDatasBtn = createToolbarButton(MaterialDesign.MDI_SWAP_HORIZONTAL, TOOLTIP_REVERSE);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 22));
        sep.setForeground(theme.separator());

        bar.add(helpBtn);
        bar.add(sep);
        bar.add(diffOnlyBtn);
        bar.add(synchronizeBtn);
        bar.add(generateFilesBtn);
        bar.add(reverseDatasBtn);
        return bar;
    }

    private JButton createToolbarButton(MaterialDesign icon, String tooltip) {
        JButton btn = new JButton(FontIcon.of(icon, 18, theme.text())) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                if (getModel().isRollover()) {
                    Color a = theme.accent();
                    g2.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JToggleButton createToolbarToggleButton(MaterialDesign icon, String tooltip) {
        JToggleButton btn = new JToggleButton(FontIcon.of(icon, 18, theme.text())) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                if (isSelected()) {
                    g2.setColor(theme.accent());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    Color a = theme.accent();
                    g2.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bottom
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(theme.bg());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(theme.separator());
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        panel.setOpaque(false);

        VButton compareBtn = new VButton("Comparer") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isEnabled()) {
                    GradientPaint grad = new GradientPaint(0, 0, theme.accent(), getWidth(), 0, theme.accentHover());
                    g2.setPaint(grad);
                } else {
                    g2.setColor(theme.cardBorder());
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        compareBtn.setContentAreaFilled(false);
        compareBtn.setBorderPainted(false);
        compareBtn.setFocusPainted(false);
        compareBtn.setForeground(Color.WHITE);
        compareBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        compareBtn.setPreferredSize(new Dimension(200, 38));
        compareBtn.setIcon(FontIcon.of(MaterialDesign.MDI_COMPARE, 18, Color.WHITE));
        compareBtn.setIconTextGap(8);
        compareBtn.setEnabled(false);
        compareBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        compareBtn.addActionListener(e -> LoadingWindow.run(parentFrame, () -> {
            treeLeft.update(controller.performComparison(), diffOnlyBtn.isSelected());
            diffOnlyBtn.setEnabled(true);
        }));

        this.compareBtn = compareBtn;
        panel.add(compareBtn);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JSplitPane createStyledSplitPane() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        sp.setResizeWeight(0.5);
        sp.setDividerSize(4);
        sp.setBorder(null);
        sp.setContinuousLayout(true);
        return sp;
    }

    private JTextField createStyledTextField() {
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

    private JScrollPane createStyledScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(theme.cardBorder(), 1));
        sp.getViewport().setBackground(theme.cardBg());
        return sp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Business logic (inchangé)
    // ─────────────────────────────────────────────────────────────────────────

    private void chooseVPIFile(Boolean isLeft) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers VPI/VPS", "vpi", "vps"));
        chooser.setCurrentDirectory(isLeft ? leftSelectedFile : rightSelectedFile);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (isLeft) { leftSelectedFile = f; leftFileField.setText(f.getAbsolutePath()); }
            else         { rightSelectedFile = f; rightFileField.setText(f.getAbsolutePath()); }
        }
    }

    private String chooseVPIFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Dossier cible");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) return chooser.getSelectedFile().getAbsolutePath();
        return null;
    }

    public void processFile(boolean isLeft) {
        try {
            if (isLeft) {
                if (leftSelectedFile == null) {
                    JOptionPane.showMessageDialog(this, "Aucun fichier sélectionné (gauche).", "Erreur", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                treeLeft.update(controller.handleFile(leftSelectedFile, TypeFile.COMPARISON_LEFT), diffOnlyBtn.isSelected());
                diffOnlyBtn.setSelected(false);
                diffOnlyBtn.setEnabled(false);
            } else {
                if (rightSelectedFile == null) {
                    JOptionPane.showMessageDialog(this, "Aucun fichier sélectionné (droite).", "Erreur", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                setGenerateFilesButtonLoading(true);
                diffOnlyBtn.setSelected(false);
                diffOnlyBtn.setEnabled(false);
                treeRight.update(controller.handleFile(rightSelectedFile, TypeFile.COMPARISON_RIGHT), diffOnlyBtn.isSelected());
                treeLeft.update(controller.getVPIData(TypeFile.COMPARISON_LEFT), diffOnlyBtn.isSelected());
                controller.getCompleteUnzipFuture(rightSelectedFile, TypeFile.COMPARISON_RIGHT)
                    .thenAccept(success -> SwingUtilities.invokeLater(() -> {
                        setGenerateFilesButtonLoading(false);
                        if (!success) System.err.println("Erreur lors du dézipage complet");
                    }));
            }
            if (controller.getVPIData(TypeFile.COMPARISON_RIGHT).isParsed()
                    && controller.getVPIData(TypeFile.COMPARISON_LEFT).isParsed()) {
                this.compareBtn.setEnabled(true);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setGenerateFilesButtonLoading(boolean loading) {
        if (loading) {
            generateFilesBtn.setEnabled(false);
            generateFilesBtn.setIcon(generateFilesIconLoading);
            generateFilesIconLoading.start(generateFilesBtn);
        } else {
            generateFilesIconLoading.stop();
            generateFilesBtn.setIcon(generateFilesIconNormal);
            generateFilesBtn.setEnabled(true);
        }
    }

    private void initSynchro() {
        TreeExpansionListener tl = createTreeExpansionListener(treeRight);
        TreeExpansionListener tr = createTreeExpansionListener(treeLeft);
        JScrollBar sl = scrollPanelLeft.getVerticalScrollBar();
        JScrollBar sr = scrollPanelRight.getVerticalScrollBar();
        AdjustmentListener al = createScrollListener(sr);
        AdjustmentListener ar = createScrollListener(sl);

        this.synchronizeBtn.setSelected(true);
        enableSynchronization(tl, tr, al, ar);

        synchronizeBtn.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) enableSynchronization(tl, tr, al, ar);
            else disableSynchronization(tl, tr, al, ar);
        });
    }

    private TreeExpansionListener createTreeExpansionListener(VPITree target) {
        return new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent e)  { target.synchronizeExpansion(e.getPath(), true);  }
            public void treeCollapsed(TreeExpansionEvent e) { target.synchronizeExpansion(e.getPath(), false); }
        };
    }

    private AdjustmentListener createScrollListener(JScrollBar target) {
        return e -> target.setValue(((JScrollBar) e.getSource()).getValue());
    }

    private void enableSynchronization(TreeExpansionListener tl, TreeExpansionListener tr,
                                       AdjustmentListener al, AdjustmentListener ar) {
        treeLeft.addTreeExpansionListener(tl);
        treeRight.addTreeExpansionListener(tr);
        scrollPanelLeft.getVerticalScrollBar().addAdjustmentListener(al);
        scrollPanelRight.getVerticalScrollBar().addAdjustmentListener(ar);
        synchronizeBtn.setIcon(FontIcon.of(MaterialDesign.MDI_SYNC, 18, theme.text()));
        synchronizeBtn.setToolTipText(TOOLTIP_DESYNCHRONIZE);
    }

    private void disableSynchronization(TreeExpansionListener tl, TreeExpansionListener tr,
                                        AdjustmentListener al, AdjustmentListener ar) {
        treeLeft.removeTreeExpansionListener(tl);
        treeRight.removeTreeExpansionListener(tr);
        scrollPanelLeft.getVerticalScrollBar().removeAdjustmentListener(al);
        scrollPanelRight.getVerticalScrollBar().removeAdjustmentListener(ar);
        synchronizeBtn.setIcon(FontIcon.of(MaterialDesign.MDI_SYNC_OFF, 18, theme.text()));
        synchronizeBtn.setToolTipText(TOOLTIP_SYNCHRONIZE);
    }

    private void initializeDiffOnlyToggle() {
        this.diffOnlyBtn.addItemListener(e -> {
            diffOnlyBtn.setToolTipText(e.getStateChange() == ItemEvent.SELECTED
                ? TOOLTIP_DISPLAY_ALL : TOOLTIP_DISPLAY_DIFFONLY);
            treeLeft.update(controller.getVPIData(TypeFile.COMPARISON_LEFT), diffOnlyBtn.isSelected());
        });
    }

    private void initializeGenerateFiles() {
        this.generateFilesBtn.addActionListener(e -> {
            String out = chooseVPIFolder();
            if (generateFilesBtn.isEnabled() && out != null)
                LoadingWindow.run(parentFrame, () -> controller.performGenerateVPI(out));
        });
    }

    private void initializeReverseDatas() {
        this.reverseDatasBtn.addActionListener(e -> {
            this.diffOnlyBtn.setSelected(false);
            controller.performReverse();
            treeLeft.update(controller.getVPIData(TypeFile.COMPARISON_LEFT), false);
            treeRight.update(controller.getVPIData(TypeFile.COMPARISON_RIGHT), false);
            String oldLeft = this.leftFileField.getText();
            this.leftFileField.setText(this.rightFileField.getText());
            this.rightFileField.setText(oldLeft);
            File oldLeftFile = this.leftSelectedFile;
            this.leftSelectedFile = this.rightSelectedFile;
            this.rightSelectedFile = oldLeftFile;
        });
    }

    private void openDocumentation() {
        new DocumentationWindow().setVisible(true);
    }
}
