package com.stilog.prism.analyzevpi.view.tabs;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

import com.stilog.prism.analyzevpi.model.history.*;
import com.stilog.prism.analyzevpi.model.history.HistoryTrackerParser;
import com.stilog.prism.analyzevpi.model.history.TrackerRef;
import com.stilog.prism.view.ThemeManager;
import com.stilog.prism.vpimodel.objects.VPIDatas;

/**
 * Onglet "Historique" du module Analyse VPI.
 *
 * <p>Chargement entièrement asynchrone (SwingWorker) en 4 étapes :
 * <ol>
 *   <li>Extraction des fichiers depuis le ZIP si nécessaire</li>
 *   <li>Chargement du {@link VpsLabelResolver} (noms lisibles depuis tout le VPS)</li>
 *   <li>Parsing de history.txt</li>
 *   <li>Parsing de historytracker.txt + jointure</li>
 * </ol>
 * Les autres onglets restent réactifs pendant toute la durée du chargement.
 */
public class VPIHistoryTab implements AbstractVPITab {

    private static final String TAB_TITLE = "Historique";
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ThemeManager theme = ThemeManager.getInstance();

    // ── UI ────────────────────────────────────────────────────────────────────
    private final JPanel      panel;
    private JLabel            loadingLabel;

    // Sous-onglets internes : Dimensions | Événements | Autres
    private JTabbedPane       innerTabs;

    // ── Filtres COMMUNS (s'appliquent à tous les sous-onglets) ────────────────
    private JTextField        commonSearchField;   // recherche texte globale
    private JComboBox<String> commonUserModCombo;  // modifié par
    private JComboBox<String> commonUserCreCombo;  // créé par
    private JComboBox<String> commonModDateOpCombo;
    private DatePickerField   commonModDatePicker;
    private JComboBox<String> commonCreDateOpCombo;
    private DatePickerField   commonCreDatePicker;
    private JLabel            commonCountLabel;

    // ── Détail partagé (PropertyChange) ──────────────────────────────────────
    private JTable            detailTable;
    private DefaultTableModel detailTableModel;
    private JLabel            detailTitleLabel;

    // ── Onglet "Dimensions" (EventResourceX) ──────────────────────────────────
    private JComboBox<String> dimTypeCombo;
    private JCheckBox         showDeletedChk;
    private JLabel            dimCountLabel;
    private JTable            dimTable;
    private HistoryTableModel dimTableModel;

    // ── Onglet "Événements" (PlanningEvent) ───────────────────────────────────
    private JComboBox<String> evtTreeCombo;
    private JComboBox<String> evtDateOpCombo;
    private DatePickerField   evtDatePicker;
    private JLabel            evtCountLabel;
    private JTable            eventsTable;
    private EventTableModel   eventsTableModel;

    // ── Onglet "Autres" (config, exports, imports…) ───────────────────────────
    private JComboBox<String> otherTypeCombo;
    private JLabel            otherCountLabel;
    private JTable            otherTable;
    private HistoryTableModel otherTableModel;

    // ── Alias legacy (compatibilité worker/onRowSelected) ─────────────────────
    private JTable            table;
    private HistoryTableModel tableModel;

    // ── Listes filtrées par onglet ────────────────────────────────────────────
    private List<HistoryEntry> dimFiltered   = Collections.emptyList();
    private List<HistoryEntry> evtFiltered   = Collections.emptyList();
    private List<HistoryEntry> otherFiltered = Collections.emptyList();
    /** Liste active (onglet visible) — utilisée par onRowSelected. */
    private List<HistoryEntry> activeFiltered = Collections.emptyList();

    // ── Données ───────────────────────────────────────────────────────────────
    private List<HistoryEntry>  allEntries   = Collections.emptyList();
    private List<HistoryEntry>  filteredList = Collections.emptyList();
    private VpsLabelResolver    resolver     = null;
    private File                workDir      = null;

    /** Worker en cours — annulé si un nouveau fichier est chargé avant la fin. */
    private SwingWorker<List<HistoryEntry>, String> activeWorker = null;

    /**
     * Fichier historytracker.txt décompressé sur disque, conservé pour la
     * lecture lazy des {@link PropertyChange} au clic utilisateur.
     */
    private File trackerFilePath = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public VPIHistoryTab() {
        panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        showPlaceholder();

        theme.addChangeListener(() -> {
            if (!allEntries.isEmpty()) rebuildUI();
            else { panel.removeAll(); showPlaceholder(); panel.revalidate(); panel.repaint(); }
        });
    }

    @Override public String getTabTitle() { return TAB_TITLE; }
    @Override public JPanel getPanel()    { return panel; }

    public void setWorkDir(File dir) { this.workDir = dir; }

    // ── Chargement asynchrone ─────────────────────────────────────────────────

    @Override
    public void onDataLoaded(VPIDatas data) {
        if (activeWorker != null && !activeWorker.isDone()) {
            activeWorker.cancel(true);
        }
        allEntries   = Collections.emptyList();
        filteredList = Collections.emptyList();
        resolver     = null;

        if (workDir == null) { showMessage("Répertoire de travail non défini."); return; }

        final File   wDir    = workDir;
        final String vpiPath = (data != null) ? data.getFilePath() : null;

        showLoading("Initialisation…");

        activeWorker = new SwingWorker<>() {

            @Override
            protected List<HistoryEntry> doInBackground() throws Exception {
                try {
                // ── Étape 1 : localiser / extraire les fichiers ───────────────
                publish("Localisation des fichiers d'historique…");
                File histFile    = new File(wDir, "history.txt");
                File trackerFile = new File(wDir, "historytracker.txt");

                File vpiFile = (vpiPath != null) ? new File(vpiPath) : null;

                if (!histFile.exists() && vpiFile != null && vpiFile.exists()) {
                    publish("Extraction depuis l'archive VPS…");
                    extractFromZip(vpiFile, "history.txt",        histFile);
                    extractFromZip(vpiFile, "historytracker.txt", trackerFile);
                }

                checkCancelled();
                if (!histFile.exists())
                    throw new FileNotFoundException("history.txt introuvable dans l'archive.");

                // ── Étape 2 : charger le résolveur de noms ────────────────────
                publish("Résolution des noms depuis le VPS…");
                VpsLabelResolver res = new VpsLabelResolver();
                // Priorité : le vpiFile source (contient tout) > le workDir
                if (vpiFile != null && vpiFile.exists()) {
                    res.load(vpiFile);
                }

                checkCancelled();

                // ── Étape 3 : parsing history.txt ─────────────────────────────
                publish("Lecture de history.txt…");
                List<HistoryEntry> entries = HistoryParser.parseHistory(histFile);

                checkCancelled();

                // ── Étape 4 : résolution des labels et noms de dimensions ────
                publish("Association des noms lisibles…");
                java.util.regex.Pattern erPattern =
                    java.util.regex.Pattern.compile("EventResource(\\d+)",
                        java.util.regex.Pattern.CASE_INSENSITIVE);
                for (HistoryEntry e : entries) {
                    // Résoudre le label par GUID d'abord (le plus précis)
                    String label = res.resolveGuid(e.getGuid());
                    // Fallback : résoudre par (type, id)
                    if (label == null) {
                        label = res.resolveTypeAndId(
                            e.getType().toLowerCase(Locale.ROOT), e.getId());
                    }
                    e.setResolvedLabel(label);

                    // Remplacer "EventResourceX" par le nom de la dimension
                    java.util.regex.Matcher m = erPattern.matcher(e.getType());
                    if (m.matches()) {
                        int rmId = Integer.parseInt(m.group(1));
                        String dimName = res.resolveDimension(rmId);
                        if (dimName != null) e.setDisplayType(dimName);
                    }

                    // Attacher le PlanningEventInfo pour le sous-onglet Événements
                    if ("PlanningEvent".equals(e.getType())) {
                        com.stilog.prism.analyzevpi.model.history.PlanningEventInfo info =
                            res.getEventInfo(e.getGuid());
                        if (info != null) {
                            e.setEventInfo(info);
                            e.setResolvedLabel(info.buildLabel());
                            e.setDisplayType(info.treeLabel());
                        }
                    }
                }

                // Log diagnostic
                long peCount  = entries.stream().filter(x -> "PlanningEvent".equals(x.getType())).count();
                long withInfo = entries.stream().filter(x -> x.getEventInfo() != null).count();
                System.err.println("[VPIHistoryTab] PlanningEvent=" + peCount
                    + " avec eventInfo=" + withInfo
                    + " eventInfoByUid=" + res.getEventInfoCount());

                checkCancelled();

                // ── Étape 5 : indexation streaming de historytracker.txt ─────
                if (trackerFile.exists()) {
                    publish("Indexation de historytracker.txt (streaming)…");
                    com.stilog.prism.analyzevpi.model.history.TrackerIndex index =
                        HistoryTrackerParser.buildIndex(trackerFile, processed -> {
                            if (!isCancelled())
                                publish("Indexation : " + (processed / 1000) + "k lignes traitées…");
                        });

                    checkCancelled();

                    publish("Jointure (" + index.totalRefs() + " entrées indexées)…");
                    HistoryParser.joinAndSort(entries, index);
                    trackerFilePath = trackerFile;
                } else {
                    trackerFilePath = null;
                    entries.sort(Comparator.comparing(
                        HistoryEntry::getDateModification,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                }

                // Stocker le resolver pour l'utiliser dans l'UI
                resolver = res;
                return entries;
                } catch (Exception e) {
                    // Relancer avec un message enrichi pour que done() puisse l'afficher
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                if (isCancelled()) return;
                updateLoadingMessage(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    List<HistoryEntry> result = get();
                    allEntries = (result != null) ? result : Collections.emptyList();
                    rebuildUI();
                } catch (java.util.concurrent.CancellationException ignored) {
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    // Logger la stack trace complète pour le diagnostic
                    cause.printStackTrace();
                    showErrorDetail(cause.getClass().getSimpleName(), buildErrorMessage(cause), cause);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            private void checkCancelled() throws InterruptedException {
                if (isCancelled()) throw new InterruptedException("Chargement annulé.");
            }
        };

        activeWorker.execute();
    }

    // ── Extraction zip ────────────────────────────────────────────────────────

    private static void extractFromZip(File zipFile, String entryName, File dest)
            throws IOException {
        try (java.util.zip.ZipInputStream zis =
                new java.util.zip.ZipInputStream(new FileInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (new File(entry.getName()).getName().equalsIgnoreCase(entryName)) {
                    dest.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(dest)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                    }
                    return;
                }
                zis.closeEntry();
            }
        }
    }

    // ── Construction UI ───────────────────────────────────────────────────────

    private void rebuildUI() {
        panel.removeAll();
        panel.setOpaque(false);
        panel.add(buildCommonFilterBar(), BorderLayout.NORTH);
        panel.add(buildMainArea(),        BorderLayout.CENTER);
        // Initialiser les combos avec les valeurs chargées
        refreshAllCombos();
        applyFilters();
        panel.revalidate();
        panel.repaint();
    }

    private void refreshAllCombos() {
        // Communs
        if (commonUserModCombo != null) commonUserModCombo.setModel(
            new javax.swing.DefaultComboBoxModel<>(collectAllUsers()));
        if (commonUserCreCombo != null) commonUserCreCombo.setModel(
            new javax.swing.DefaultComboBoxModel<>(collectAllUsers()));
        // Spécifiques
        if (dimTypeCombo   != null) dimTypeCombo.setModel(
            new javax.swing.DefaultComboBoxModel<>(collectDimTypes()));
        if (evtTreeCombo   != null) evtTreeCombo.setModel(
            new javax.swing.DefaultComboBoxModel<>(collectTreeNames()));
        if (otherTypeCombo != null) otherTypeCombo.setModel(
            new javax.swing.DefaultComboBoxModel<>(collectOtherTypes()));
    }

    // ── Écrans de statut ──────────────────────────────────────────────────────

    private void showPlaceholder() {
        panel.removeAll();
        JLabel lbl = new JLabel(
            "Importez un VPI/VPS dans l'onglet « Arbre » pour afficher l'historique.",
            SwingConstants.CENTER);
        lbl.setForeground(theme.textDim());
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC, 14f));
        panel.add(lbl, BorderLayout.CENTER);
        panel.revalidate(); panel.repaint();
    }

    private void showMessage(String msg) {
        panel.removeAll();
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setForeground(theme.textDim());
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC, 13f));
        panel.add(lbl, BorderLayout.CENTER);
        panel.revalidate(); panel.repaint();
    }

    private void showLoading(String initialMessage) {
        panel.removeAll();
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(Box.createVerticalGlue());

        JProgressBar spinner = new JProgressBar();
        spinner.setIndeterminate(true);
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        spinner.setMaximumSize(new Dimension(280, 6));
        spinner.setPreferredSize(new Dimension(280, 6));

        loadingLabel = new JLabel(initialMessage, SwingConstants.CENTER);
        loadingLabel.setForeground(theme.textDim());
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(Font.ITALIC, 13f));
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadingLabel.setBorder(new EmptyBorder(12, 0, 0, 0));

        center.add(spinner);
        center.add(loadingLabel);
        center.add(Box.createVerticalGlue());

        panel.add(center, BorderLayout.CENTER);
        panel.revalidate(); panel.repaint();
    }

    private void updateLoadingMessage(String message) {
        if (loadingLabel != null) loadingLabel.setText(message);
    }

    // ── Barre de recherche / filtres ──────────────────────────────────────────

    // ── Barre de filtres communs ─────────────────────────────────────────────

    /**
     * Construit la barre de filtres communs, affichée en permanence au-dessus
     * des sous-onglets. S'applique à Dimensions, Événements et Autres.
     * Filtres : recherche globale | modifié par | créé par |
     *           date modif. | date création
     */
    private JPanel buildCommonFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(theme.cardBg());
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, theme.cardBorder()),
            new EmptyBorder(6, 10, 6, 10)));

        // Ligne 1 : recherche + compteur global
        JPanel row1 = new JPanel(new BorderLayout(8, 0));
        row1.setOpaque(false);
        commonSearchField = new JTextField();
        commonSearchField.putClientProperty("JTextField.placeholderText",
            "Rechercher dans tout l'historique…");
        styleField(commonSearchField);
        commonSearchField.getDocument().addDocumentListener(docListener(() -> applyFilters()));
        commonCountLabel = new JLabel();
        commonCountLabel.setForeground(theme.textDim());
        commonCountLabel.setFont(commonCountLabel.getFont().deriveFont(11f));
        commonCountLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
        row1.add(new JLabel("  "), BorderLayout.WEST);
        row1.add(commonSearchField, BorderLayout.CENTER);
        row1.add(commonCountLabel,  BorderLayout.EAST);

        // Ligne 2 : modifié par | créé par
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2.setOpaque(false);
        row2.add(labelOf("Modifié par :"));
        commonUserModCombo = buildCombo(collectAllUsers());
        commonUserModCombo.addActionListener(e -> applyFilters());
        row2.add(commonUserModCombo);
        row2.add(labelOf("Créé par :"));
        commonUserCreCombo = buildCombo(collectAllUsers());
        commonUserCreCombo.addActionListener(e -> applyFilters());
        row2.add(commonUserCreCombo);

        showDeletedChk = new JCheckBox("Afficher supprimés");
        showDeletedChk.setForeground(theme.textDim());
        showDeletedChk.setOpaque(false);
        showDeletedChk.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        showDeletedChk.addActionListener(e -> applyFilters());
        row2.add(showDeletedChk);

        // Ligne 3 : date modif + date création
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row3.setOpaque(false);
        row3.add(labelOf("Modifié :"));
        commonModDateOpCombo = buildCombo(new String[]{
            "(aucun)", "le", "après le", "avant le",
            "cette semaine", "ce mois", "cette année"
        });
        commonModDateOpCombo.addActionListener(e -> {
            boolean needs = needsDateValue((String) commonModDateOpCombo.getSelectedItem());
            commonModDatePicker.setEnabled(needs);
            if (!needs) commonModDatePicker.clear();
            applyFilters();
        });
        row3.add(commonModDateOpCombo);
        commonModDatePicker = new DatePickerField();
        commonModDatePicker.setEnabled(false);
        commonModDatePicker.addDateChangeListener(() -> applyFilters());
        row3.add(commonModDatePicker);

        row3.add(Box.createHorizontalStrut(16));
        row3.add(labelOf("Créé :"));
        commonCreDateOpCombo = buildCombo(new String[]{
            "(aucun)", "le", "après le", "avant le",
            "cette semaine", "ce mois", "cette année"
        });
        commonCreDateOpCombo.addActionListener(e -> {
            boolean needs = needsDateValue((String) commonCreDateOpCombo.getSelectedItem());
            commonCreDatePicker.setEnabled(needs);
            if (!needs) commonCreDatePicker.clear();
            applyFilters();
        });
        row3.add(commonCreDateOpCombo);
        commonCreDatePicker = new DatePickerField();
        commonCreDatePicker.setEnabled(false);
        commonCreDatePicker.addDateChangeListener(() -> applyFilters());
        row3.add(commonCreDatePicker);

        JButton resetBtn = makeBarButton("↺ Tout réinitialiser");
        resetBtn.addActionListener(e -> resetAllFilters());
        row3.add(resetBtn);

        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 2));
        rows.setOpaque(false);
        rows.add(row1); rows.add(row2); rows.add(row3);
        bar.add(rows, BorderLayout.CENTER);
        return bar;
    }

    private void resetAllFilters() {
        // Communs
        if (commonSearchField    != null) commonSearchField.setText("");
        if (commonUserModCombo   != null) commonUserModCombo.setSelectedIndex(0);
        if (commonUserCreCombo   != null) commonUserCreCombo.setSelectedIndex(0);
        if (commonModDateOpCombo != null) commonModDateOpCombo.setSelectedIndex(0);
        if (commonModDatePicker  != null) { commonModDatePicker.clear(); commonModDatePicker.setEnabled(false); }
        if (commonCreDateOpCombo != null) commonCreDateOpCombo.setSelectedIndex(0);
        if (commonCreDatePicker  != null) { commonCreDatePicker.clear(); commonCreDatePicker.setEnabled(false); }
        // Spécifiques
        resetDimFilters();
        resetEvtFilters();
        resetOtherFilters();
    }

    // ── Barre de filtres par onglet ──────────────────────────────────────────

    /**
     * Construit la barre de filtres de l'onglet "Dimensions".
     * Filtres : recherche texte | type de dimension | utilisateur |
     *           afficher supprimés | date (modif ou création)
     */
    private JPanel buildDimFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(theme.cardBg());
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));

        // Ligne 1 : recherche
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row1.setOpaque(false);
        dimCountLabel = new JLabel("0 entrées");
        dimCountLabel.setForeground(theme.textDim());
        dimCountLabel.setFont(dimCountLabel.getFont().deriveFont(11f));
        row1.add(dimCountLabel);

        // Ligne 2 : type + user + supprimés
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2.setOpaque(false);
        row2.add(labelOf("Dimension :"));
        dimTypeCombo = buildCombo(collectDimTypes());
        dimTypeCombo.addActionListener(e -> applyFilters());
        row2.add(dimTypeCombo);

        JButton resetDim = makeBarButton("↺");
        resetDim.addActionListener(e -> resetDimFilters());
        row2.add(resetDim);

        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 2));
        rows.setOpaque(false);
        rows.add(row1); rows.add(row2);
        bar.add(rows, BorderLayout.CENTER);
        return bar;
    }

    /**
     * Construit la barre de filtres de l'onglet "Événements".
     * Filtres : recherche | type d'arbre | utilisateur | dates début/fin
     */
    private JPanel buildEvtFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(theme.cardBg());
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));

        // Ligne 1 : recherche
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row1.setOpaque(false);
        evtCountLabel = new JLabel("0 entrées");
        evtCountLabel.setForeground(theme.textDim());
        evtCountLabel.setFont(evtCountLabel.getFont().deriveFont(11f));
        row1.add(evtCountLabel);

        // Ligne 2 : type d'arbre + user
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2.setOpaque(false);
        row2.add(labelOf("Type d'arbre :"));
        evtTreeCombo = buildCombo(collectTreeNames());
        evtTreeCombo.addActionListener(e -> applyFilters());
        row2.add(evtTreeCombo);


        // Ligne 3 : filtre sur dates de l'événement (début ou fin)
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row3.setOpaque(false);
        row3.add(labelOf("Date event :"));
        evtDateOpCombo = buildCombo(new String[]{
            "(aucun)", "commence après le", "commence avant le",
            "finit après le", "finit avant le",
            "actif le", "cette semaine", "ce mois"
        });
        evtDateOpCombo.addActionListener(e -> {
            boolean needs = needsDateValue((String) evtDateOpCombo.getSelectedItem());
            evtDatePicker.setEnabled(needs);
            if (!needs) evtDatePicker.clear();
            applyFilters();
        });
        row3.add(evtDateOpCombo);
        evtDatePicker = new DatePickerField();
        evtDatePicker.setEnabled(false);
        evtDatePicker.addDateChangeListener(() -> applyFilters());
        row3.add(evtDatePicker);
        JButton resetEvt = makeBarButton("↺");
        resetEvt.addActionListener(e -> resetEvtFilters());
        row3.add(resetEvt);

        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 2));
        rows.setOpaque(false);
        rows.add(row1); rows.add(row2); rows.add(row3);
        bar.add(rows, BorderLayout.CENTER);
        return bar;
    }

    /**
     * Construit la barre de filtres de l'onglet "Autres".
     * Filtres simples : recherche + type.
     */
    private JPanel buildOtherFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(theme.cardBg());
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);


        row.add(labelOf("Type :"));
        otherTypeCombo = buildCombo(collectOtherTypes());
        otherTypeCombo.addActionListener(e -> applyFilters());
        row.add(otherTypeCombo);

        otherCountLabel = new JLabel();
        otherCountLabel.setForeground(theme.textDim());
        otherCountLabel.setFont(otherCountLabel.getFont().deriveFont(11f));
        row.add(otherCountLabel);

        JButton resetOther = makeBarButton("↺");
        resetOther.addActionListener(e -> resetOtherFilters());
        row.add(resetOther);

        bar.add(row, BorderLayout.CENTER);
        return bar;
    }

    private boolean needsDateValue(String op) {
        return op != null && (op.contains("le") || op.contains("Le")) && !op.equals("(aucun)");
    }

    private void resetDimFilters() {
        if (dimTypeCombo    != null) dimTypeCombo.setSelectedIndex(0);
        if (showDeletedChk    != null) showDeletedChk.setSelected(false);
    }

    private void resetEvtFilters() {
        if (evtTreeCombo   != null) evtTreeCombo.setSelectedIndex(0);
        if (evtDateOpCombo != null) evtDateOpCombo.setSelectedIndex(0);
        if (evtDatePicker  != null) { evtDatePicker.clear(); evtDatePicker.setEnabled(false); }
    }

    private void resetOtherFilters() {
        if (otherTypeCombo   != null) otherTypeCombo.setSelectedIndex(0);
    }

    // ── Construction de la zone principale ───────────────────────────────────

    private JComponent buildMainArea() {
        innerTabs = new JTabbedPane(JTabbedPane.TOP);
        innerTabs.setFont(innerTabs.getFont().deriveFont(12f));
        innerTabs.addTab("Dimensions",  buildDimTab());
        innerTabs.addTab("Événements",  buildEventsTab());
        innerTabs.addTab("Autres",      buildOtherTab());

        // Mettre à jour activeFiltered quand l'utilisateur change d'onglet
        innerTabs.addChangeListener(e -> {
            int idx = innerTabs.getSelectedIndex();
            activeFiltered = switch (idx) {
                case 0 -> dimFiltered;
                case 1 -> evtFiltered;
                case 2 -> otherFiltered;
                default -> Collections.emptyList();
            };
            clearDetail();
        });

        // ── Panel de détail partagé ───────────────────────────────────────────
        detailTitleLabel = new JLabel(
            "  Sélectionnez une ligne pour voir le détail des modifications");
        detailTitleLabel.setFont(detailTitleLabel.getFont().deriveFont(Font.BOLD, 12f));
        detailTitleLabel.setForeground(theme.textDim());
        detailTitleLabel.setBorder(new EmptyBorder(6, 8, 4, 8));

        detailTableModel = new DefaultTableModel(
            new String[]{ "#", "Date", "Utilisateur", "Propriété",
                          "Ancienne valeur", "Nouvelle valeur" }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailTableModel);
        styleTable(detailTable);
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(135);
        detailTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        detailTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        detailTable.getColumnModel().getColumn(4).setPreferredWidth(190);
        detailTable.getColumnModel().getColumn(5).setPreferredWidth(190);
        // Renderer : col[0]==null → ligne de groupe (fond teinté, gras)
        DefaultTableCellRenderer detailRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                boolean isGroup = t.getValueAt(row, 0) == null;
                if (isGroup) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    if (!sel) {
                        Color bg = theme.cardBg(), ac = theme.accent();
                        setBackground(new Color(
                            (bg.getRed()*3+ac.getRed())/4,
                            (bg.getGreen()*3+ac.getGreen())/4,
                            (bg.getBlue()*3+ac.getBlue())/4));
                        setForeground(theme.text());
                    }
                    setBorder(BorderFactory.createMatteBorder(1,0,0,0,theme.cardBorder()));
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    if (!sel) { setBackground(theme.cardBg()); setForeground(theme.text()); }
                    setBorder(new EmptyBorder(0,4,0,0));
                }
                return this;
            }
        };
        for (int i = 0; i < 6; i++)
            detailTable.getColumnModel().getColumn(i).setCellRenderer(detailRenderer);

        JScrollPane detailScroll = new JScrollPane(detailTable);
        styleScroll(detailScroll);
        detailScroll.setPreferredSize(new Dimension(0, 185));

        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(theme.cardBg());
        detailPanel.setBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, theme.cardBorder()));
        detailPanel.add(detailTitleLabel, BorderLayout.NORTH);
        detailPanel.add(detailScroll,     BorderLayout.CENTER);

        // ── Split global : onglets en haut, détail en bas ─────────────────────
        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, innerTabs, detailPanel);
        split.setResizeWeight(0.70);
        split.setBorder(null);
        split.setDividerSize(5);
        return split;
    }

    /** Onglet Dimensions : ressources (EventResourceX). */
    private JComponent buildDimTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);
        wrapper.add(buildDimFilterBar(), BorderLayout.NORTH);

        dimTableModel = new HistoryTableModel(Collections.emptyList());
        dimTable      = new JTable(dimTableModel);
        table      = dimTable;
        tableModel = dimTableModel;
        styleTable(dimTable);
        dimTable.setAutoCreateRowSorter(true);
        int[] widths = { 50, 220, 120, 130, 80, 130, 80, 55 };
        for (int i = 0; i < widths.length && i < dimTable.getColumnCount(); i++)
            dimTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        dimTable.getColumnModel().getColumn(1).setCellRenderer(new LabelCellRenderer());
        dimTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        JScrollPane scroll = new JScrollPane(dimTable);
        styleScroll(scroll);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    /** Onglet Événements : PlanningEvent. */
    private JComponent buildEventsTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);
        wrapper.add(buildEvtFilterBar(), BorderLayout.NORTH);
        eventsTableModel = new EventTableModel(Collections.emptyList());
        eventsTable = new JTable(eventsTableModel);
        styleTable(eventsTable);
        eventsTable.setAutoCreateRowSorter(true);
        int[] widths = { 55, 160, 320, 120, 120, 55, 90, 55 };
        for (int i = 0; i < widths.length && i < eventsTable.getColumnCount(); i++)
            eventsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        eventsTable.getColumnModel().getColumn(2).setCellRenderer(new LabelCellRenderer() {
            @Override protected boolean isResolved(int modelRow) {
                return modelRow < eventsTableModel.getRowCount()
                    && eventsTableModel.getEntry(modelRow).isResolved();
            }
        });
        eventsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });
        JScrollPane scroll = new JScrollPane(eventsTable);
        styleScroll(scroll);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    /** Onglet Autres : configuration, exports, imports... */
    private JComponent buildOtherTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);
        wrapper.add(buildOtherFilterBar(), BorderLayout.NORTH);

        otherTableModel = new HistoryTableModel(Collections.emptyList());
        otherTable = new JTable(otherTableModel);
        styleTable(otherTable);
        otherTable.setAutoCreateRowSorter(true);
        int[] widths = { 50, 260, 130, 130, 80, 130, 80, 55 };
        for (int i = 0; i < widths.length && i < otherTable.getColumnCount(); i++)
            otherTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        otherTable.getColumnModel().getColumn(1).setCellRenderer(new LabelCellRenderer());
        otherTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        JScrollPane scroll = new JScrollPane(otherTable);
        styleScroll(scroll);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

        private void applyFilters() {
        java.time.LocalDate today = java.time.LocalDate.now();

        // ── Filtres communs ──────────────────────────────────────────────────
        String commonSearch = commonSearchField    != null ? commonSearchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        String userMod      = commonUserModCombo   != null ? (String) commonUserModCombo.getSelectedItem()  : null;
        String userCre      = commonUserCreCombo   != null ? (String) commonUserCreCombo.getSelectedItem()  : null;
        String modDateOp    = commonModDateOpCombo != null ? (String) commonModDateOpCombo.getSelectedItem() : null;
        java.time.LocalDate modRef = commonModDatePicker != null ? commonModDatePicker.getDate() : null;
        String creDateOp    = commonCreDateOpCombo != null ? (String) commonCreDateOpCombo.getSelectedItem() : null;
        java.time.LocalDate creRef = commonCreDatePicker != null ? commonCreDatePicker.getDate() : null;

        boolean showDel = showDeletedChk != null && showDeletedChk.isSelected();

        // Prédicat commun — s'applique à toutes les catégories
        java.util.function.Predicate<HistoryEntry> commonPred = e -> {
            if (!showDel && !e.isResolved()) return false;
            if (userMod != null && !userMod.isEmpty() && !"(Tous)".equals(userMod)
                    && !userMod.equals(e.getUserModification())) return false;
            if (userCre != null && !userCre.isEmpty() && !"(Tous)".equals(userCre)
                    && !userCre.equals(e.getUserCreation())) return false;
            if (!matchesDateFilter(e.getDateModification(), modDateOp, modRef, today)) return false;
            if (!matchesDateFilter(e.getDateCreation(),     creDateOp, creRef, today)) return false;
            if (!commonSearch.isEmpty()) {
                String blob = (e.getDisplayLabel() + " " + e.getDisplayType() + " "
                    + e.getUserModification() + " " + e.getUserCreation() + " "
                    + e.getId()).toLowerCase(Locale.ROOT);
                if (!blob.contains(commonSearch)) return false;
            }
            return true;
        };

        // ── Séparer par catégorie ────────────────────────────────────────────
        List<HistoryEntry> dimAll   = allEntries.stream()
            .filter(e -> e.getType() != null && e.getType().matches("EventResource\\d+"))
            .toList();
        List<HistoryEntry> evtAll   = allEntries.stream()
            .filter(e -> "PlanningEvent".equals(e.getType()) || "PlanningEventLink".equals(e.getType()))
            .toList();
        List<HistoryEntry> otherAll = allEntries.stream()
            .filter(e -> { String t = e.getType();
                return t != null && !t.matches("EventResource\\d+")
                    && !"PlanningEvent".equals(t) && !"PlanningEventLink".equals(t); })
            .toList();

        // ── Filtres Dimensions ───────────────────────────────────────────────
        String dimType  = dimTypeCombo      != null ? (String) dimTypeCombo.getSelectedItem()  : null;

        dimFiltered = dimAll.stream()
            .filter(commonPred)
            .filter(e -> {
                if (dimType != null && !dimType.isEmpty() && !"(Tous)".equals(dimType)
                        && !dimType.equals(e.getDisplayType())) return false;
                return true;
            }).collect(Collectors.toList());

        if (dimTableModel  != null) dimTableModel.setEntries(dimFiltered);
        if (dimCountLabel != null) dimCountLabel.setText(dimFiltered.size() + " / " + dimAll.size());

        // ── Filtres Événements ───────────────────────────────────────────────
        String evtTree   = evtTreeCombo   != null ? (String) evtTreeCombo.getSelectedItem()  : null;
        String evtDateOp = evtDateOpCombo != null ? (String) evtDateOpCombo.getSelectedItem() : null;
        java.time.LocalDate evtRef = evtDatePicker != null ? evtDatePicker.getDate() : null;

        evtFiltered = evtAll.stream()
            .filter(commonPred)
            .filter(e -> {
                if (evtTree != null && !evtTree.isEmpty() && !"(Tous)".equals(evtTree)
                        && !evtTree.equals(e.getDisplayType())) return false;
                if (!matchesEvtDateFilter(e, evtDateOp, evtRef, today)) return false;
                return true;
            }).collect(Collectors.toList());

        if (eventsTableModel != null) eventsTableModel.setEntries(evtFiltered);
        if (evtCountLabel    != null) evtCountLabel.setText(evtFiltered.size() + " / " + evtAll.size());

        // ── Filtres Autres ───────────────────────────────────────────────────
        String otherType = otherTypeCombo != null ? (String) otherTypeCombo.getSelectedItem() : null;

        otherFiltered = otherAll.stream()
            .filter(commonPred)
            .filter(e -> {
                if (otherType != null && !otherType.isEmpty() && !"(Tous)".equals(otherType)
                        && !otherType.equals(e.getDisplayType())) return false;
                return true;
            }).collect(Collectors.toList());

        if (otherTableModel  != null) otherTableModel.setEntries(otherFiltered);
        if (otherCountLabel != null) otherCountLabel.setText(otherFiltered.size() + " / " + otherAll.size()); // grouped

        // ── Titres onglets + compteur global ─────────────────────────────────
        int total = dimFiltered.size() + evtFiltered.size() + otherFiltered.size();
        int totalAll = dimAll.size() + evtAll.size() + otherAll.size();
        if (commonCountLabel != null)
            commonCountLabel.setText(total + " / " + totalAll + " entrées");
        if (innerTabs != null) {
            innerTabs.setTitleAt(0, "Dimensions ("  + dimFiltered.size()   + ")");
            innerTabs.setTitleAt(1, "Événements ("  + evtFiltered.size()   + ")");
            innerTabs.setTitleAt(2, "Autres ("      + otherFiltered.size() + ")");
        }

        // Mettre à jour activeFiltered selon l'onglet visible
        if (innerTabs != null) {
            activeFiltered = switch (innerTabs.getSelectedIndex()) {
                case 0 -> dimFiltered;
                case 1 -> evtFiltered;
                case 2 -> otherFiltered;
                default -> Collections.emptyList();
            };
        }
        filteredList = activeFiltered; // alias legacy
        clearDetail();
    }

    /** Filtre sur date de modification (onglet Dimensions). */
    private boolean matchesDateFilter(java.time.LocalDateTime dt, String op,
                                      java.time.LocalDate ref, java.time.LocalDate today) {
        if (op == null || op.isEmpty() || op.equals("(aucun)")) return true;
        if (dt == null) return false;
        java.time.LocalDate d = dt.toLocalDate();
        return switch (op) {
            case "le jour"      -> ref != null && d.equals(ref);
            case "après le"     -> ref != null && d.isAfter(ref);
            case "avant le"     -> ref != null && d.isBefore(ref);
            case "cette semaine"-> !d.isBefore(today.with(java.time.DayOfWeek.MONDAY)) && !d.isAfter(today);
            case "ce mois"      -> d.getYear() == today.getYear() && d.getMonth() == today.getMonth();
            case "cette année"  -> d.getYear() == today.getYear();
            default             -> true;
        };
    }

    /** Filtre sur dates de l'événement (début/fin). */
    private boolean matchesEvtDateFilter(HistoryEntry e, String op,
                                         java.time.LocalDate ref, java.time.LocalDate today) {
        if (op == null || op.isEmpty() || op.equals("(aucun)")) return true;
        com.stilog.prism.analyzevpi.model.history.PlanningEventInfo info = e.getEventInfo();
        if (info == null) return true; // pas de dates → pas de filtre
        java.time.LocalDateTime begin = info.beginDate();
        java.time.LocalDateTime end   = info.endDate();
        java.time.LocalDate refD = ref != null ? ref : today;
        return switch (op) {
            case "commence après le"  -> begin != null && begin.toLocalDate().isAfter(refD);
            case "commence avant le"  -> begin != null && begin.toLocalDate().isBefore(refD);
            case "finit après le"     -> end   != null && end.toLocalDate().isAfter(refD);
            case "finit avant le"     -> end   != null && end.toLocalDate().isBefore(refD);
            case "actif le"           -> {
                yield begin != null && end != null
                    && !begin.toLocalDate().isAfter(refD)
                    && !end.toLocalDate().isBefore(refD);
            }
            case "cette semaine"      -> {
                java.time.LocalDate mon = today.with(java.time.DayOfWeek.MONDAY);
                java.time.LocalDate sun = mon.plusDays(6);
                yield begin != null
                    && !begin.toLocalDate().isAfter(sun)
                    && !begin.toLocalDate().isBefore(mon);
            }
            case "ce mois"            -> {
                yield begin != null
                    && begin.getYear() == today.getYear()
                    && begin.getMonth() == today.getMonth();
            }
            default                   -> true;
        };
    }

    private void onRowSelected() {
        // Trouver la table active selon l'onglet courant
        JTable activeTable = switch (innerTabs != null ? innerTabs.getSelectedIndex() : 0) {
            case 1  -> eventsTable;
            case 2  -> otherTable;
            default -> dimTable;
        };
        if (activeTable == null) return;

        int viewRow = activeTable.getSelectedRow();
        if (viewRow < 0) { clearDetail(); return; }

        // Pour les tables groupées, récupérer l'entrée directement
        HistoryEntry entry = null;
        int selectedIndex = innerTabs != null ? innerTabs.getSelectedIndex() : 0;
        List<HistoryEntry> source = switch (selectedIndex) {
            case 1  -> evtFiltered;
            case 2  -> otherFiltered;
            default -> dimFiltered;
        };
        int modelRow = activeTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= source.size()) { clearDetail(); return; }
        entry = source.get(modelRow);
        if (entry == null) { clearDetail(); return; }

        String labelStr = entry.isResolved()
            ? "  " + entry.getDisplayLabel() + "   —   " + entry.getType() + " #" + entry.getId()
            : "  " + entry.getType() + " #" + entry.getId();

        detailTitleLabel.setText(labelStr
            + "   —   GUID : " + entry.getGuid()
            + "   —   " + entry.getTrackerCount() + " modification(s)");
        detailTitleLabel.setForeground(theme.text());

        detailTableModel.setRowCount(0);

        if (entry.getTrackerRefs().isEmpty()) {
            detailTitleLabel.setText(labelStr + "  —  Aucune modification détaillée disponible");
            detailTitleLabel.setForeground(theme.textDim());
            return;
        }

        final File tf = trackerFilePath;
        if (tf == null || !tf.exists()) {
            detailTitleLabel.setText(labelStr + "  —  Fichier tracker non disponible");
            detailTitleLabel.setForeground(theme.textDim());
            return;
        }

        // Regrouper par date+user : si plusieurs TrackerRef ont la même date,
        // insérer un header de groupe (col[0]=null comme marqueur)
        String lastGroupKey = null;
        int idx = 0;
        for (TrackerRef ref : entry.getTrackerRefs()) {
            List<PropertyChange> changes = HistoryTrackerParser.loadChanges(tf, ref);
            if (changes.isEmpty()) continue;

            String dateStr  = ref.date() != null ? ref.date().format(DT_FMT) : "?";
            String groupKey = dateStr + "|" + ref.user();

            if (!groupKey.equals(lastGroupKey)) {
                // Ligne séparateur de groupe
                detailTableModel.addRow(new Object[]{
                    null,          // marqueur groupe (null → renderer coloré)
                    dateStr,
                    ref.user(),
                    "",
                    "",
                    ""
                });
                lastGroupKey = groupKey;
            }

            for (PropertyChange c : changes) {
                detailTableModel.addRow(new Object[]{
                    ++idx,
                    "",
                    "",
                    c.getPropertyName(),
                    truncate(c.getOldValue(), 300),
                    truncate(c.getNewValue(), 300)
                });
            }
        }
        if (detailTableModel.getRowCount() == 0) {
            detailTitleLabel.setText(labelStr + "  —  Aucune propriété modifiée trouvée");
            detailTitleLabel.setForeground(theme.textDim());
        }
    }

    private void clearDetail() {
        if (detailTitleLabel != null) {
            detailTitleLabel.setText(
                "  Sélectionnez une ligne pour voir le détail des modifications");
            detailTitleLabel.setForeground(theme.textDim());
        }
        if (detailTableModel != null) detailTableModel.setRowCount(0);
    }

    // ── Collecte des valeurs de filtre ────────────────────────────────────────

    private String[] collectTypes() { return collectDimTypes(); } // legacy

    private String[] collectAllUsers() {
        List<String> users = new ArrayList<>();
        users.add("(Tous)");
        allEntries.stream()
            .flatMap(e -> java.util.stream.Stream.of(e.getUserCreation(), e.getUserModification()))
            .filter(u -> u != null && !u.isBlank())
            .distinct().sorted().forEach(users::add);
        return users.toArray(new String[0]);
    }

    private String[] collectDimTypes() {
        List<String> types = new ArrayList<>();
        types.add("(Tous)");
        allEntries.stream()
            .filter(e -> e.getType() != null && e.getType().matches("EventResource\\d+"))
            .map(HistoryEntry::getDisplayType)
            .filter(t -> t != null && !t.isBlank())
            .distinct().sorted().forEach(types::add);
        return types.toArray(new String[0]);
    }

    private String[] collectDimUsers() {
        List<String> users = new ArrayList<>();
        users.add("(Tous)");
        allEntries.stream()
            .filter(e -> e.getType() != null && e.getType().matches("EventResource\\d+"))
            .flatMap(e -> java.util.stream.Stream.of(e.getUserCreation(), e.getUserModification()))
            .filter(u -> u != null && !u.isBlank())
            .distinct().sorted().forEach(users::add);
        return users.toArray(new String[0]);
    }

    private String[] collectTreeNames() {
        List<String> trees = new ArrayList<>();
        trees.add("(Tous)");
        allEntries.stream()
            .filter(e -> "PlanningEvent".equals(e.getType()) || "PlanningEventLink".equals(e.getType()))
            .map(HistoryEntry::getDisplayType)
            .filter(t -> t != null && !t.isBlank() && !"PlanningEvent".equals(t))
            .distinct().sorted().forEach(trees::add);
        return trees.toArray(new String[0]);
    }

    private String[] collectEvtUsers() {
        List<String> users = new ArrayList<>();
        users.add("(Tous)");
        allEntries.stream()
            .filter(e -> "PlanningEvent".equals(e.getType()))
            .map(HistoryEntry::getUserModification)
            .filter(u -> u != null && !u.isBlank())
            .distinct().sorted().forEach(users::add);
        return users.toArray(new String[0]);
    }

    private String[] collectOtherTypes() {
        List<String> types = new ArrayList<>();
        types.add("(Tous)");
        allEntries.stream()
            .filter(e -> {
                String t = e.getType();
                return t != null && !t.matches("EventResource\\d+")
                    && !"PlanningEvent".equals(t) && !"PlanningEventLink".equals(t);
            })
            .map(HistoryEntry::getDisplayType)
            .filter(t -> t != null && !t.isBlank())
            .distinct().sorted().forEach(types::add);
        return types.toArray(new String[0]);
    }

    private String[] collectUsers() { return collectDimUsers(); } // legacy

    // ── TableModel ────────────────────────────────────────────────────────────

    // Colonnes : ID | Nom | Type | Date création | Créé par | Date modif. | Modifié par | Modifs.
    private static final String[] COLUMNS = {
        "ID", "Nom", "Type", "Date création", "Créé par", "Date modif.", "Modifié par", "Modifs."
    };

    static class HistoryTableModel extends AbstractTableModel {
        private List<HistoryEntry> entries;

        HistoryTableModel(List<HistoryEntry> entries) { this.entries = entries; }

        void setEntries(List<HistoryEntry> e) { this.entries = e; fireTableDataChanged(); }

        @Override public int    getRowCount()        { return entries.size(); }
        @Override public int    getColumnCount()     { return COLUMNS.length; }
        @Override public String getColumnName(int c) { return COLUMNS[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) { case 0, 7 -> Integer.class; default -> String.class; };
        }

        @Override
        public Object getValueAt(int r, int c) {
            HistoryEntry e = entries.get(r);
            return switch (c) {
                case 0 -> e.getId();
                case 1 -> e.getDisplayLabel();
                case 2 -> e.getDisplayType();
                case 3 -> e.getDateCreation()     != null ? e.getDateCreation().format(DT_FMT)     : "";
                case 4 -> e.getUserCreation();
                case 5 -> e.getDateModification() != null ? e.getDateModification().format(DT_FMT) : "";
                case 6 -> e.getUserModification();
                case 7 -> e.getTrackerCount();
                default -> "";
            };
        }

        HistoryEntry getEntry(int modelRow) {
            return entries.get(modelRow);
        }
    }

    // ── Renderer coloré pour la colonne Nom ───────────────────────────────────

    /**
     * Affiche le label en couleur accentuée si résolu, en gris italique sinon.
     */
    private class LabelCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            int modelRow = t.convertRowIndexToModel(row);
            if (isResolved(modelRow)) {
                if (!sel) setForeground(theme.text());
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                if (!sel) setForeground(theme.textDim());
                setFont(getFont().deriveFont(Font.ITALIC));
            }
            return this;
        }

        /** Peut être surchargé par les sous-onglets avec leur propre model. */
        protected boolean isResolved(int modelRow) {
            if (tableModel == null || modelRow >= tableModel.getRowCount()) return false;
            return tableModel.getEntry(modelRow).isResolved();
        }
    }

    // ── Gestion des erreurs ──────────────────────────────────────────────────

    /**
     * Construit un message d'erreur lisible même quand {@code getMessage()} est null.
     * Remonte la chaîne de causes pour trouver le premier message non-null.
     */
    private static String buildErrorMessage(Throwable t) {
        Throwable current = t;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && !msg.isBlank()) return msg;
            current = current.getCause();
        }
        // Aucun message → afficher le type + première ligne de la stack
        StackTraceElement[] stack = t.getStackTrace();
        String location = (stack.length > 0)
            ? " à " + stack[0].getClassName() + "." + stack[0].getMethodName()
              + ":" + stack[0].getLineNumber()
            : "";
        return t.getClass().getName() + location;
    }

    /**
     * Affiche un panneau d'erreur avec le type d'exception, le message,
     * et les premières lignes de la stack trace pour faciliter le diagnostic.
     */
    private void showErrorDetail(String exceptionType, String message, Throwable cause) {
        panel.removeAll();
        panel.setOpaque(false);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Titre
        JLabel title = new JLabel("⚠  Erreur lors du chargement de l'historique");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(200, 80, 60));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(title);
        center.add(Box.createVerticalStrut(10));

        // Type + message
        JLabel typeLbl = new JLabel(exceptionType + " : " + message);
        typeLbl.setFont(typeLbl.getFont().deriveFont(Font.PLAIN, 12f));
        typeLbl.setForeground(theme.text());
        typeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(typeLbl);
        center.add(Box.createVerticalStrut(14));

        // Stack trace (10 premières lignes)
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stack = cause.getStackTrace();
        int limit = Math.min(stack.length, 10);
        for (int i = 0; i < limit; i++) {
            sb.append("  at ").append(stack[i]).append("\n");
        }
        if (stack.length > limit) sb.append("  ... (").append(stack.length - limit).append(" more)");

        JTextArea stackArea = new JTextArea(sb.toString());
        stackArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        stackArea.setForeground(theme.textDim());
        stackArea.setBackground(theme.cardBg());
        stackArea.setEditable(false);
        stackArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.cardBorder(), 1),
            new EmptyBorder(6, 8, 6, 8)));
        stackArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        stackArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        center.add(stackArea);
        center.add(Box.createVerticalStrut(12));

        JLabel hint = new JLabel("La stack trace complète est disponible dans la console.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        hint.setForeground(theme.textDim());
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(hint);

        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(theme.bg());
        panel.add(scroll, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }

    // ── Sous-onglet Événements ────────────────────────────────────────────────

    // buildEventsSubTab() supprimé — inline dans buildEventsTab()


    /** @deprecated Logique inline dans {@link #applyFilters()}. */
    private void refreshEventsTab() { /* no-op : géré par applyFilters() */ }

    // ── EventTableModel ───────────────────────────────────────────────────────

    private static final String[] EVENT_COLUMNS = {
        "ID", "Type d'arbre", "Ressources clés", "Début", "Fin", "Durée (j)", "Modifié par", "Modifs."
    };

    static class EventTableModel extends AbstractTableModel {
        private List<HistoryEntry> entries;

        EventTableModel(List<HistoryEntry> e) { this.entries = e; }

        void setEntries(List<HistoryEntry> e) { this.entries = e; fireTableDataChanged(); }

        @Override public int    getRowCount()        { return entries.size(); }
        @Override public int    getColumnCount()     { return EVENT_COLUMNS.length; }
        @Override public String getColumnName(int c) { return EVENT_COLUMNS[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) { case 0, 5, 7 -> Integer.class; default -> String.class; };
        }

        @Override
        public Object getValueAt(int r, int c) {
            HistoryEntry e = entries.get(r);
            com.stilog.prism.analyzevpi.model.history.PlanningEventInfo info = e.getEventInfo();
            return switch (c) {
                case 0 -> e.getId();
                case 1 -> info != null ? info.treeLabel() : "PlanningEvent";
                case 2 -> info != null ? buildResourcesKey(info) : e.getDisplayLabel();
                case 3 -> info != null && info.beginDate() != null
                    ? info.beginDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                case 4 -> info != null && info.endDate() != null
                    ? info.endDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                case 5 -> info != null ? info.durationDays() : null;
                case 6 -> e.getUserModification();
                case 7 -> e.getTrackerCount();
                default -> "";
            };
        }

        HistoryEntry getEntry(int modelRow) { return entries.get(modelRow); }

        private String buildResourcesKey(
                com.stilog.prism.analyzevpi.model.history.PlanningEventInfo info) {
            if (info.treeStruct() != null) {
                // Dimensions obligatoires dans l'ordre
                StringBuilder sb = new StringBuilder();
                for (var d : info.treeStruct().getIdentificationDims()) {
                    String label = info.resourceLabels().get(d.rmId());
                    if (label != null) {
                        if (!sb.isEmpty()) sb.append(" | ");
                        sb.append(label);
                    }
                }
                if (!sb.isEmpty()) return sb.toString();
            }
            // Fallback : toutes les ressources
            return String.join(" | ", info.resourceLabels().values());
        }
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private void styleTable(JTable t) {
        t.setBackground(theme.cardBg());
        t.setForeground(theme.text());
        t.setGridColor(theme.cardBorder());
        t.setRowHeight(22);
        t.setSelectionBackground(theme.accent());
        t.setSelectionForeground(Color.WHITE);
        t.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        t.getTableHeader().setBackground(theme.cardBg());
        t.getTableHeader().setForeground(theme.textDim());
        t.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        t.setFillsViewportHeight(true);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(8, 1));
    }

    private void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(theme.cardBorder(), 1));
        sp.getViewport().setBackground(theme.cardBg());
    }

    private void styleField(JTextField f) {
        f.setBackground(theme.fieldBg());
        f.setForeground(theme.text());
        f.setCaretColor(theme.text());
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.fieldBorder(), 1),
            new EmptyBorder(3, 8, 3, 8)));
    }

    private JComboBox<String> buildCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(theme.fieldBg());
        cb.setForeground(theme.text());
        cb.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        return cb;
    }

    private JLabel labelOf(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(theme.textDim());
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        return l;
    }

    private JButton makeBarButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(theme.textDim());
        b.setBackground(theme.fieldBg());
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.fieldBorder(), 1),
            new EmptyBorder(3, 10, 3, 10)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        return b;
    }

    private static DocumentListener docListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { r.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
