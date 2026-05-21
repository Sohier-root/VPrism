package com.stilog.prism.view;

import java.awt.*;
import javax.swing.*;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

import com.stilog.prism.analyzevpi.controller.SingleVPIController;
import com.stilog.prism.analyzevpi.model.SingleVPIModel;
import com.stilog.prism.analyzevpi.view.SingleVPIMainPage;
import com.stilog.prism.comparevpi.controller.ComparisonController;
import com.stilog.prism.comparevpi.model.ComparisonModel;
import com.stilog.prism.comparevpi.utils.SystemInfo;
import com.stilog.prism.comparevpi.view.ComparisonMainPage;
import com.stilog.prism.view.documentation.DocumentationWindow;

public class MainFrame extends JFrame {

    private final SystemInfo infos = SystemInfo.getInstance();
    private final ThemeManager theme = ThemeManager.getInstance();

    private JPanel contentPanel;
    private JPanel header;
    private JButton btnModule1;
    private JButton btnModule2;
    private JButton themeToggleBtn;
    private JButton docBtn;
    private int activeModule = 1;

    // Instances conservées pour ne pas perdre les données au changement de thème
    private ComparisonMainPage comparisonPage;
    private SingleVPIMainPage singleVPIPage;

    private final Runnable themeListener = this::onThemeChanged;

    public MainFrame() {
        super("VPrism");
        this.setTitle(this.getTitle() + " (" + infos.getVersion() + ")");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        header = buildHeader();
        add(header, BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        switchToModule(getOrCreateComparisonPage());

        theme.addChangeListener(themeListener);
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(theme.headerBg());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(theme.headerBorder());
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        h.setPreferredSize(new Dimension(0, 52));

        // Center: module nav
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        navPanel.setOpaque(false);
        btnModule1 = buildNavButton("Comparaison", MaterialDesign.MDI_VECTOR_DIFFERENCE, 1);
        btnModule2 = buildNavButton("Analyse VPI",  MaterialDesign.MDI_SITEMAP, 2);
        navPanel.add(btnModule1);
        navPanel.add(btnModule2);

        // Right: theme toggle + settings + quit
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setOpaque(false);

        themeToggleBtn = buildThemeToggleButton();
        docBtn = buildIconButton(MaterialDesign.MDI_HELP_CIRCLE, "Aide et documentation", theme.textDim());
        docBtn.addActionListener(e -> new DocumentationWindow().setVisible(true));
        JButton settingsBtn = buildIconButton(MaterialDesign.MDI_SETTINGS, "Paramètres", theme.textDim());
        settingsBtn.addActionListener(e -> showSettingsMenu(settingsBtn));
        JButton quitBtn = buildIconButton(MaterialDesign.MDI_POWER, "Quitter", new Color(200, 70, 70));
        quitBtn.addActionListener(e -> System.exit(0));

        rightPanel.add(themeToggleBtn);
        rightPanel.add(docBtn);
        rightPanel.add(settingsBtn);
        rightPanel.add(quitBtn);

        h.add(navPanel,   BorderLayout.CENTER);
        h.add(rightPanel, BorderLayout.EAST);

        updateNavButtons();
        return h;
    }

    private JButton buildNavButton(String label, MaterialDesign icon, int moduleIndex) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (activeModule == moduleIndex) {
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
        btn.setIcon(FontIcon.of(icon, 18, theme.navInactive()));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(theme.navInactive());
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(6);
        btn.addActionListener(e -> {
            activeModule = moduleIndex;
            updateNavButtons();
            switchToModule(moduleIndex == 1 ? getOrCreateComparisonPage() : getOrCreateSingleVPIPage());
        });
        return btn;
    }

    private JButton buildThemeToggleButton() {
        MaterialDesign icon = theme.isDark()
            ? MaterialDesign.MDI_WHITE_BALANCE_SUNNY
            : MaterialDesign.MDI_WEATHER_NIGHT;
        String tooltip = theme.isDark() ? "Passer en thème clair" : "Passer en thème sombre";
        JButton btn = buildIconButton(icon, tooltip, theme.textDim());
        btn.addActionListener(e -> theme.toggle(SwingUtilities.getWindowAncestor(this)));
        return btn;
    }

    private JButton buildIconButton(MaterialDesign icon, String tooltip, Color iconColor) {
        JButton btn = new JButton(FontIcon.of(icon, 18, iconColor)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        btn.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showSettingsMenu(JButton anchor) {
        JPopupMenu menu = new JPopupMenu();

        String themeLabel = theme.isDark() ? "☀  Thème clair" : "☾  Thème sombre";
        JMenuItem themeItem = new JMenuItem(themeLabel);
        themeItem.addActionListener(e -> theme.toggle(SwingUtilities.getWindowAncestor(this)));
        menu.add(themeItem);

        menu.addSeparator();

        JMenuItem quitItem = new JMenuItem("Quitter");
        quitItem.addActionListener(e -> System.exit(0));
        menu.add(quitItem);

        menu.show(anchor, 0, anchor.getHeight());
    }

    private void updateNavButtons() {
        boolean m1 = (activeModule == 1);
        Color active   = Color.WHITE;
        Color inactive = theme.navInactive();
        btnModule1.setForeground(m1 ? active : inactive);
        btnModule2.setForeground(m1 ? inactive : active);
        btnModule1.setIcon(FontIcon.of(MaterialDesign.MDI_VECTOR_DIFFERENCE, 18, m1 ? active : inactive));
        btnModule2.setIcon(FontIcon.of(MaterialDesign.MDI_SITEMAP, 18, m1 ? inactive : active));
        btnModule1.repaint();
        btnModule2.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Theme reaction
    // ─────────────────────────────────────────────────────────────────────────

    private void onThemeChanged() {
        header.repaint();

        // Bouton toggle thème
        MaterialDesign icon = theme.isDark()
            ? MaterialDesign.MDI_WHITE_BALANCE_SUNNY
            : MaterialDesign.MDI_WEATHER_NIGHT;
        themeToggleBtn.setIcon(FontIcon.of(icon, 18, theme.textDim()));
        themeToggleBtn.setToolTipText(theme.isDark() ? "Passer en thème clair" : "Passer en thème sombre");

        docBtn.setIcon(FontIcon.of(MaterialDesign.MDI_HELP_CIRCLE, 18, theme.textDim()));

        updateNavButtons();

        // Juste un repaint — les données sont conservées
        contentPanel.repaint();
        if (comparisonPage != null) SwingUtilities.updateComponentTreeUI(comparisonPage);
        if (singleVPIPage  != null) SwingUtilities.updateComponentTreeUI(singleVPIPage);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Module switching
    // ─────────────────────────────────────────────────────────────────────────

    public void switchToModule(JPanel modulePanel) {
        contentPanel.removeAll();
        contentPanel.add(modulePanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private ComparisonMainPage getOrCreateComparisonPage() {
        if (comparisonPage == null) {
            comparisonPage = new ComparisonMainPage(
                new ComparisonController(new ComparisonModel()), this);
        }
        return comparisonPage;
    }

    private SingleVPIMainPage getOrCreateSingleVPIPage() {
        if (singleVPIPage == null) {
            singleVPIPage = new SingleVPIMainPage(
                new SingleVPIController(new SingleVPIModel()), this);
        }
        return singleVPIPage;
    }

    // Gardées pour compatibilité si appelées depuis ailleurs
    public ComparisonMainPage createComparisonPage() {
        return getOrCreateComparisonPage();
    }

    public SingleVPIMainPage createSingleVPIPage() {
        return getOrCreateSingleVPIPage();
    }
}
