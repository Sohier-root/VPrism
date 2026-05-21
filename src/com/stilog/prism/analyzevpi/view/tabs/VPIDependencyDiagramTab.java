package com.stilog.prism.analyzevpi.view.tabs;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.stilog.prism.comparevpi.model.GeneralCorrespondance;
import com.stilog.prism.view.ThemeManager;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.TypeData;
import com.stilog.prism.vpimodel.objects.VPIDatas;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;


public class VPIDependencyDiagramTab implements AbstractVPITab {

    private static final String TAB_TITLE = "Dépendances";

    private final ThemeManager theme = ThemeManager.getInstance();

    // ── Couleurs des nœuds — 8 types ─────────────────────────────────────────
    private static final Color C_DIM          = new Color( 52, 120, 215);
    private static final Color C_DIM_B        = new Color(100, 165, 255);
    private static final Color C_HIER         = new Color( 34, 160, 100);
    private static final Color C_HIER_B       = new Color( 80, 210, 145);
    private static final Color C_FILT_RES     = new Color(200, 100,  30);
    private static final Color C_FILT_RES_B   = new Color(245, 150,  65);
    private static final Color C_FILT_EVT     = new Color(190,  45,  80);
    private static final Color C_FILT_EVT_B   = new Color(235,  90, 120);
    private static final Color C_IMP_RES      = new Color( 90,  55, 175);
    private static final Color C_IMP_RES_B    = new Color(150, 110, 240);
    private static final Color C_IMP_EVT      = new Color( 20, 130, 185);
    private static final Color C_IMP_EVT_B    = new Color( 65, 195, 235);
    private static final Color C_EXP_RES      = new Color(155,  50, 155);
    private static final Color C_EXP_RES_B    = new Color(210, 105, 210);
    private static final Color C_EXP_EVT      = new Color( 15, 160, 130);
    private static final Color C_EXP_EVT_B    = new Color( 55, 215, 175);
    private static final Color C_EDGE_HL      = new Color(255, 210,  55, 230);
    private static final Color C_SEL_RING     = new Color(255, 225,  55);

    // Couleurs d'accent pour les zones de groupe
    private static final Color C_GRP_FILTER   = new Color(200,  80,  50);   // rouge-orange
    private static final Color C_GRP_IMPORT   = new Color( 60,  80, 190);   // bleu-violet
    private static final Color C_GRP_EXPORT   = new Color( 50, 160, 120);   // vert-teal

    // ── Couleurs thème-dépendantes ────────────────────────────────────────────
    private Color cBg()            { return theme.isDark() ? new Color(26, 30, 40)       : new Color(232, 236, 244); }
    private Color cGrid()          { return theme.isDark() ? new Color(38, 44, 58)       : new Color(212, 218, 230); }
    private Color cEdge()          { return theme.isDark() ? new Color(110,120,145,150)  : new Color(80, 92,125,120); }
    private Color cText()          { return theme.isDark() ? Color.WHITE                 : new Color(12, 18, 32); }
    private Color cTextDim()       { return theme.isDark() ? new Color(175, 188, 210)    : new Color(65, 78, 108); }
    private Color cBarBg()         { return theme.isDark() ? new Color(34, 39, 53)       : new Color(255, 255, 255); }
    private Color cFieldBg()       { return theme.isDark() ? new Color(48, 55, 72)       : new Color(244, 247, 252); }
    private Color cFieldBorder()   { return theme.isDark() ? new Color(78, 90, 120)      : new Color(192, 200, 218); }
    private Color cTooltipBg()     { return theme.isDark() ? new Color(16, 20, 32, 228)  : new Color(28, 33, 50, 232); }
    private Color cTooltipBorder() { return theme.isDark() ? new Color(78, 90, 130)      : new Color(145, 158, 190); }
    private Color cColBorder()     { return theme.isDark() ? new Color(55, 65, 95, 75)   : new Color(165, 180, 212, 85); }

    // ── Géométrie ─────────────────────────────────────────────────────────────
    private static final int NW       = 165;   // largeur nœud
    private static final int NH       = 38;    // hauteur nœud
    private static final int ARC      = 9;
    private static final int COL_GAP  = 210;   // espacement standard entre colonnes
    private static final int GRP_GAP  = 80;    // espacement supplémentaire AVANT un groupe
    private static final int ROW_GAP  = 20;
    private static final int COL_PAD  = 10;    // padding interne zone colonne individuelle
    private static final int GRP_PAD  = 22;    // padding zone de groupe englobante

    // ── UI ────────────────────────────────────────────────────────────────────
    private final JPanel  panel;
    private DiagramCanvas canvas;
    private JTextField    searchField;
    private JLabel        infoLabel;
    private DiagramModel  lastModel;

    public VPIDependencyDiagramTab() {
        panel = new JPanel(new BorderLayout());
        panel.setBackground(cBg());
        showPlaceholder();
        theme.addChangeListener(() -> {
            panel.setBackground(cBg());
            if (lastModel != null) rebuildUI();
            else { panel.removeAll(); showPlaceholder(); panel.revalidate(); panel.repaint(); }
        });
    }

    @Override public String getTabTitle() { return TAB_TITLE; }
    @Override public JPanel getPanel()    { return panel; }

    @Override
    public void onDataLoaded(VPIDatas data) {
        lastModel = buildModel(data);
        layoutColumns(lastModel);
        rebuildUI();
    }

    private void rebuildUI() {
        String currentSearch = (searchField != null) ? searchField.getText() : "";
        panel.removeAll();
        panel.setBackground(cBg());
        panel.add(buildTopBar(), BorderLayout.NORTH);
        canvas = new DiagramCanvas(lastModel);
        panel.add(canvas,        BorderLayout.CENTER);
        panel.add(buildLegend(), BorderLayout.SOUTH);
        if (!currentSearch.isEmpty() && searchField != null) searchField.setText(currentSearch);
        panel.revalidate();
        panel.repaint();
    }

    private void showPlaceholder() {
        JLabel lbl = new JLabel(
            "Importez un VPI/VPS dans l'onglet « Arbre » pour afficher le diagramme.",
            SwingConstants.CENTER);
        lbl.setForeground(cTextDim());
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC, 14f));
        panel.add(lbl, BorderLayout.CENTER);
    }

    // ── Barre de recherche ────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(cBarBg());
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher un nœud…");
        searchField.setBackground(cFieldBg());
        searchField.setForeground(cText());
        searchField.setCaretColor(cText());
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cFieldBorder(), 1),
            new EmptyBorder(3, 8, 3, 8)));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { onSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { onSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
        });

        JButton resetBtn = makeBarButton("⌖  Reset vue");
        resetBtn.addActionListener(e -> { if (canvas != null) canvas.resetView(); });

        infoLabel = new JLabel(" ");
        infoLabel.setForeground(cTextDim());
        infoLabel.setFont(infoLabel.getFont().deriveFont(11f));

        bar.add(infoLabel,   BorderLayout.WEST);
        bar.add(searchField, BorderLayout.CENTER);
        bar.add(resetBtn,    BorderLayout.EAST);
        return bar;
    }

    private JButton makeBarButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(cTextDim());
        b.setBackground(cFieldBg());
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cFieldBorder(), 1),
            new EmptyBorder(3, 12, 3, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        return b;
    }

    private void onSearch() {
        if (canvas == null) return;
        canvas.setFilter(searchField.getText().trim().toLowerCase());
        canvas.repaint();
    }

    // ── Légende ───────────────────────────────────────────────────────────────

    private JPanel buildLegend() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(cBarBg());
        outer.setBorder(new EmptyBorder(2, 8, 4, 8));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 3));
        row1.setOpaque(false);
        addLegendItem(row1, C_DIM,       "Dimension");
        addLegendItem(row1, C_HIER,      "Hiérarchie");
        addSep(row1);
        addLegendItem(row1, C_FILT_RES,  "Filtre Ressource");
        addLegendItem(row1, C_FILT_EVT,  "Filtre Événement");
        addSep(row1);
        addLegendItem(row1, C_IMP_RES,   "Import Ressource");
        addLegendItem(row1, C_IMP_EVT,   "Import Événement");
        addSep(row1);
        addLegendItem(row1, C_EXP_RES,   "Export Ressource");
        addLegendItem(row1, C_EXP_EVT,   "Export Événement");

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row2.setOpaque(false);
        JLabel hint = new JLabel("Clic : sélectionner  ·  Molette : zoom  ·  Glisser : pan");
        hint.setForeground(cTextDim());
        hint.setFont(hint.getFont().deriveFont(10.5f));
        row2.add(hint);

        outer.add(row1, BorderLayout.CENTER);
        outer.add(row2, BorderLayout.SOUTH);
        return outer;
    }

    private void addLegendItem(JPanel p, Color c, String label) {
        final Color cc = c;
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cc);
                g2.fillRoundRect(0, (getHeight()-13)/2, 20, 13, 4, 4);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(22, 16); }
        };
        dot.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(cTextDim());
        lbl.setFont(lbl.getFont().deriveFont(11f));
        JPanel entry = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        entry.setOpaque(false);
        entry.add(dot); entry.add(lbl);
        p.add(entry);
    }

    private void addSep(JPanel p) {
        JLabel sep = new JLabel("│");
        sep.setForeground(cTextDim());
        sep.setFont(sep.getFont().deriveFont(13f));
        p.add(sep);
    }

    // =========================================================================
    // Modèle de données
    // =========================================================================

    /** 8 types distincts : Import et Export séparés, chacun décliné Ressource/Événement. */
    enum NodeType {
        DIMENSION,
        HIERARCHY,
        FILTER_RESOURCE,
        FILTER_EVENT,
        IMPORT_RESOURCE,
        IMPORT_EVENT,
        EXPORT_RESOURCE,
        EXPORT_EVENT
    }

    static class DNode {
        final String id, label; final NodeType type;
        final Entity entity;
        double x, y; int rank;
        DNode(String id, String label, NodeType type, Entity entity) {
            this.id=id; this.label=label; this.type=type; this.entity=entity;
        }
    }

    static class DEdge {
        final String fromId, toId;
        DEdge(String f, String t) { fromId=f; toId=t; }
    }

    /**
     * colBounds  : bounding-box de chaque colonne individuelle (fond teinté + titre)
     * grpBounds  : bounding-box des zones de groupe (rectangle englobant Filtres / Imports / Exports)
     */
    static class DiagramModel {
        final Map<String, DNode>       nodes     = new LinkedHashMap<>();
        final List<DEdge>              edges     = new ArrayList<>();
        final Map<NodeType,Rectangle2D> colBounds = new LinkedHashMap<>();
        // Clé de groupe : "FILTER", "IMPORT", "EXPORT"
        final Map<String,Rectangle2D>  grpBounds = new LinkedHashMap<>();
    }

    // ── Construction du modèle ────────────────────────────────────────────────

    private DiagramModel buildModel(VPIDatas data) {
        DiagramModel m = new DiagramModel();

        for (FileDatas fd : data.getFilesDatas()) {
            if (fd == null) continue;
            for (Entity entity : fd.getEntities()) {
                NodeType nt  = resolveType(entity, fd.getName());
                String   nid = nt.name() + "::" + entity.getName();
                m.nodes.putIfAbsent(nid, new DNode(nid, entity.getName(), nt, entity));
            }
        }

        for (FileDatas fd : data.getFilesDatas()) {
            if (fd == null) continue;
            for (Entity entity : fd.getEntities()) {
                NodeType nt  = resolveType(entity, fd.getName());
                String   nid = nt.name() + "::" + entity.getName();

                if (nt == NodeType.DIMENSION) {
                    for (Parameters param : entity.getParameters()) {
                        String refDim = param.getAttributeValue(VPIConstants.PARAMETER_RESOURCEMODEL);
                        if (refDim != null && !refDim.isBlank()) linkToDimension(m, nid, refDim);
                    }
                } else if (nt == NodeType.HIERARCHY) {
                    for (Parameters param : entity.getParameters()) {
                        linkToDimension(m, nid, param.getName());
                        String condXml = param.getAttributeValue(VPIConstants.PARAMETER_CONDITIONS);
                        if (condXml != null && !condXml.isBlank()) {
                            for (String dimId : extractDimIdsFromConditionXml(condXml)) {
                                String dimName = GeneralCorrespondance.getInstance()
                                    .getCorrespondance(VPIConstants.PARAMETER_RESOURCEMODEL, dimId);
                                if (dimName != null && !dimName.isBlank()) linkToDimension(m, nid, dimName);
                            }
                        }
                    }
                } else if (nt == NodeType.IMPORT_RESOURCE || nt == NodeType.IMPORT_EVENT
                        || nt == NodeType.EXPORT_RESOURCE || nt == NodeType.EXPORT_EVENT) {
                    for (Parameters param : entity.getParameters()) {
                        // Lien principal : dimension cible via PARAMETER_PARAMETRE/PARAMETER_RESOURCEMODEL
                        if (VPIConstants.PARAMETER_PARAMETRE.equals(param.getName())) {
                            String dimName = param.getAttributeValue(VPIConstants.PARAMETER_RESOURCEMODEL);
                            if (dimName != null && !dimName.isBlank()) linkToDimension(m, nid, dimName);
                        }
                        // Liens secondaires : dimensions référencées dans les correspondances
                        // via les attributs "__DIM" ajoutés par computeCorrespondance
                        if (VPIConstants.PARAMETER_CORRESPONDANCE.equals(param.getName())) {
                            for (com.stilog.prism.vpimodel.objects.Attribute attr : param.getHiddenAttributes()) {
                                if (attr.getKey().endsWith(VPIConstants.CORRESPONDANCE_DIM_SUFFIX)) {
                                    String dimName = attr.getValue();
                                    if (dimName != null && !dimName.isBlank()) linkToDimension(m, nid, dimName);
                                }
                            }
                        }
                    }
                } else if (nt == NodeType.FILTER_RESOURCE || nt == NodeType.FILTER_EVENT) {
                    for (Parameters param : entity.getParameters()) {
                        String condXml = param.getAttributeValue(VPIConstants.PARAMETER_CONDITIONS);
                        if (condXml != null && !condXml.isBlank()) {
                            extractDimIdsFromConditionXml(condXml).forEach(dimId -> {
                                String dimName = GeneralCorrespondance.getInstance()
                                    .getCorrespondance(VPIConstants.PARAMETER_RESOURCEMODEL, dimId);
                                if (dimName != null && !dimName.isBlank()) linkToDimension(m, nid, dimName);
                            });
                        }
                    }
                }
            }
        }
        return m;
    }

    private Set<String> extractDimIdsFromConditionXml(String xml) {
        Set<String> ids = new LinkedHashSet<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader("<root>" + xml + "</root>")));
            NodeList rms = doc.getElementsByTagName("resourceModel");
            for (int i = 0; i < rms.getLength(); i++) {
                NodeList ch = rms.item(i).getChildNodes();
                for (int j = 0; j < ch.getLength(); j++) {
                    if ("entityID".equals(ch.item(j).getNodeName())) {
                        String id = ch.item(j).getTextContent().trim();
                        if (!id.isBlank() && !"-1".equals(id)) ids.add(id);
                    }
                }
            }
        } catch (Exception ignored) {}
        return ids;
    }

    private void linkToDimension(DiagramModel m, String fromId, String dimName) {
        if (dimName == null || dimName.isBlank()) return;
        String dimId = NodeType.DIMENSION.name() + "::" + dimName;
        if (m.nodes.containsKey(dimId) && !fromId.equals(dimId)) addEdgeIfAbsent(m, fromId, dimId);
    }

    private void addEdgeIfAbsent(DiagramModel m, String from, String to) {
        for (DEdge e : m.edges) if (e.fromId.equals(from) && e.toId.equals(to)) return;
        m.edges.add(new DEdge(from, to));
    }

    private NodeType resolveType(Entity entity, String fdName) {
        if (entity.getTypeData() == TypeData.DIMENSION)                  return NodeType.DIMENSION;
        if (fdName.equals(VPIConstants.NAME_TREE_EVENTSSTRUCT))          return NodeType.HIERARCHY;
        if (fdName.equals(VPIConstants.NAME_TREE_RESOURCESFILTER))       return NodeType.FILTER_RESOURCE;
        if (fdName.equals(VPIConstants.NAME_TREE_EVENTSFILTER))          return NodeType.FILTER_EVENT;
        if (fdName.equals(VPIConstants.NAME_TREE_RESOURCESIMPORT))       return NodeType.IMPORT_RESOURCE;
        if (fdName.equals(VPIConstants.NAME_TREE_EVENTSIMPORT))          return NodeType.IMPORT_EVENT;
        if (fdName.equals(VPIConstants.NAME_TREE_RESOURCESEXPORT))       return NodeType.EXPORT_RESOURCE;
        if (fdName.equals(VPIConstants.NAME_TREE_EVENTSEXPORT))          return NodeType.EXPORT_EVENT;
        return NodeType.DIMENSION;
    }

    // ── Layout en 8 colonnes avec espacement de groupe ────────────────────────

    /**
     * Ordre des colonnes et décalage X supplémentaire avant chaque colonne.
     * Le GRP_GAP est injecté avant la 1re colonne de chaque groupe.
     */
    private static final NodeType[] COL_ORDER = {
        NodeType.DIMENSION,
        NodeType.HIERARCHY,
        NodeType.FILTER_RESOURCE,   // +GRP_GAP avant cette colonne
        NodeType.FILTER_EVENT,
        NodeType.IMPORT_RESOURCE,   // +GRP_GAP avant cette colonne
        NodeType.IMPORT_EVENT,
        NodeType.EXPORT_RESOURCE,   // +GRP_GAP avant cette colonne
        NodeType.EXPORT_EVENT
    };

    /** Colonnes qui déclenchent un espacement de groupe avant elles. */
    private static final Set<NodeType> GROUP_STARTS = new HashSet<>(Arrays.asList(
        NodeType.FILTER_RESOURCE, NodeType.IMPORT_RESOURCE, NodeType.EXPORT_RESOURCE
    ));

    private void layoutColumns(DiagramModel m) {
        Map<NodeType, List<DNode>> cols = new LinkedHashMap<>();
        for (NodeType t : COL_ORDER) cols.put(t, new ArrayList<>());
        for (DNode n : m.nodes.values()) cols.get(n.type).add(n);

        // Tri alpha + rang initial
        for (List<DNode> list : cols.values()) {
            list.sort(Comparator.comparing(n -> n.label.toLowerCase(Locale.ROOT)));
            for (int i = 0; i < list.size(); i++) list.get(i).rank = i;
        }

        // Heuristique barycentre (3 passes)
        for (int pass = 0; pass < 3; pass++) {
            for (int c = 1; c < COL_ORDER.length; c++) {
                List<DNode> cur  = cols.get(COL_ORDER[c]);
                List<DNode> prev = cols.get(COL_ORDER[c-1]);
                Map<String,Integer> prevRank = new HashMap<>();
                for (DNode n : prev) prevRank.put(n.id, n.rank);
                Map<String,Double> bary = new HashMap<>();
                for (DNode n : cur) {
                    List<Integer> ranks = new ArrayList<>();
                    for (DEdge e : m.edges) {
                        if (e.fromId.equals(n.id) && prevRank.containsKey(e.toId))   ranks.add(prevRank.get(e.toId));
                        if (e.toId.equals(n.id)   && prevRank.containsKey(e.fromId)) ranks.add(prevRank.get(e.fromId));
                    }
                    bary.put(n.id, ranks.isEmpty() ? (double)n.rank
                        : ranks.stream().mapToInt(i->i).average().orElse(n.rank));
                }
                cur.sort(Comparator.comparingDouble(n -> bary.getOrDefault(n.id,(double)n.rank)));
                for (int i = 0; i < cur.size(); i++) cur.get(i).rank = i;
            }
        }

        // Coordonnées — calcul du X avec décalage de groupe
        int maxRows = cols.values().stream().mapToInt(List::size).max().orElse(1);
        int totalH  = maxRows * (NH + ROW_GAP);
        m.colBounds.clear();

        int currentX = 40;
        Map<NodeType, Integer> colX = new LinkedHashMap<>();
        for (NodeType t : COL_ORDER) {
            if (GROUP_STARTS.contains(t)) currentX += GRP_GAP;
            colX.put(t, currentX);
            currentX += COL_GAP;
        }

        for (NodeType t : COL_ORDER) {
            List<DNode> list = cols.get(t);
            int x = colX.get(t);
            if (!list.isEmpty()) {
                int colH   = list.size() * (NH + ROW_GAP) - ROW_GAP;
                int startY = 60 + (totalH - colH) / 2;   // 60 = marge haute pour les étiquettes groupe
                for (DNode n : list) { n.x = x; n.y = startY + n.rank * (NH + ROW_GAP); }
                m.colBounds.put(t, new Rectangle2D.Double(
                    x - COL_PAD,
                    startY - COL_PAD - 20,
                    NW + COL_PAD * 2,
                    colH + COL_PAD * 2 + 20));
            }
        }

        // Zones de groupe englobantes
        m.grpBounds.clear();
        computeGroupBound(m, "FILTER", new NodeType[]{ NodeType.FILTER_RESOURCE, NodeType.FILTER_EVENT }, colX, totalH, cols);
        computeGroupBound(m, "IMPORT", new NodeType[]{ NodeType.IMPORT_RESOURCE, NodeType.IMPORT_EVENT }, colX, totalH, cols);
        computeGroupBound(m, "EXPORT", new NodeType[]{ NodeType.EXPORT_RESOURCE, NodeType.EXPORT_EVENT }, colX, totalH, cols);
    }

    private void computeGroupBound(DiagramModel m, String key, NodeType[] members,
                                   Map<NodeType,Integer> colX, int totalH,
                                   Map<NodeType, List<DNode>> cols) {
        // Vérifier qu'au moins une colonne membre est non vide
        boolean anyNonEmpty = false;
        for (NodeType t : members) if (!cols.get(t).isEmpty()) { anyNonEmpty = true; break; }
        if (!anyNonEmpty) return;

        int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE;
        for (NodeType t : members) {
            if (!cols.get(t).isEmpty()) {
                xMin = Math.min(xMin, colX.get(t));
                xMax = Math.max(xMax, colX.get(t) + NW);
            }
        }

        int grpH = totalH + COL_PAD * 2;
        int grpY = 60 - GRP_PAD - 4;   // aligne avec les fonds de colonnes

        m.grpBounds.put(key, new Rectangle2D.Double(
            xMin - GRP_PAD,
            grpY,
            (xMax - xMin) + GRP_PAD * 2,
            grpH + GRP_PAD + 12));
    }

    // =========================================================================
    // Canvas interactif
    // =========================================================================

    class DiagramCanvas extends JPanel {

        private final DiagramModel model;
        private double scale=1.0, tx=40, ty=30;
        private Point  dragPt; private double dragTX, dragTY;
        private String selectedId=null, hoveredId=null, filterText="";

        /**
         * Mode "focus" : seuls le nœud focalisé et ses voisins directs sont visibles.
         * Activé via clic droit → "Afficher uniquement les connexions".
         * Réinitialisé par clic gauche sur le nœud focalisé ou sur le vide.
         */
        private String focusedId=null;

        DiagramCanvas(DiagramModel model) {
            this.model = model;
            setBackground(cBg());
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)         { onPress(e); }
                @Override public void mouseDragged(MouseEvent e)         { onDrag(e); }
                @Override public void mouseReleased(MouseEvent e)        { setCursor(Cursor.getDefaultCursor()); }
                @Override public void mouseClicked(MouseEvent e)         { onClick(e); }
                @Override public void mouseMoved(MouseEvent e)           { onMove(e); }
                @Override public void mouseWheelMoved(MouseWheelEvent e) { onWheel(e); }
            };
            addMouseListener(ma); addMouseMotionListener(ma); addMouseWheelListener(ma);
        }

        void setFilter(String f) { filterText=f; selectedId=null; focusedId=null; repaint(); }
        void resetView()          { scale=1.0; tx=40; ty=30; repaint(); }

        /**
         * En mode focus, calcule les Y resserrés pour chaque nœud visible.
         * Chaque colonne est recentrée verticalement, en conservant l'ordre relatif.
         */
        private Map<String,Double> computeFocusPositions(Set<String> focusSet) {
            if (focusSet == null) return null;
            Map<NodeType, List<DNode>> byCol = new LinkedHashMap<>();
            for (NodeType t : COL_ORDER) byCol.put(t, new ArrayList<>());
            for (DNode n : model.nodes.values())
                if (focusSet.contains(n.id)) byCol.get(n.type).add(n);
            for (List<DNode> list : byCol.values())
                list.sort(Comparator.comparingDouble(n -> n.y));

            // Ancrer le centre vertical sur la position Y du nœud focalisé lui-même
            DNode anchor = model.nodes.get(focusedId);
            double centerY = (anchor != null) ? anchor.y + NH / 2.0 : 200;

            Map<String,Double> focusY = new HashMap<>();
            for (List<DNode> list : byCol.values()) {
                if (list.isEmpty()) continue;
                double colH  = list.size() * (NH + ROW_GAP) - ROW_GAP;
                double startY = centerY - colH / 2.0;
                for (int i = 0; i < list.size(); i++)
                    focusY.put(list.get(i).id, startY + i * (NH + ROW_GAP));
            }
            return focusY;
        }

        /** Retourne un nœud avec Y remplacé, sans modifier l'original. */
        private DNode withY(DNode n, Double newY) {
            if (newY == null) return n;
            DNode tmp = new DNode(n.id, n.label, n.type, n.entity);
            tmp.x = n.x; tmp.y = newY; tmp.rank = n.rank;
            return tmp;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            drawGrid(g);
            g.translate(tx, ty);
            g.scale(scale, scale);

            Set<String> hl=highlighted();
            Set<String> focusSet = focusedId!=null ? buildNeighbours(focusedId) : null;
            Map<String,Double> focusY = computeFocusPositions(focusSet);
            boolean hasSel=selectedId!=null, hasFilter=!filterText.isEmpty(), hasFocus=focusedId!=null;

            // 1) Zones de groupe (dessinées en premier, sous tout le reste)
            drawGroupBackgrounds(g, focusSet, focusY);
            // 2) Fonds de colonnes individuelles
            drawColumnBackgrounds(g, focusSet, focusY);

            // 3) Arêtes
            for (DEdge e : model.edges) {
                DNode from=model.nodes.get(e.fromId), to=model.nodes.get(e.toId);
                if (from==null||to==null) continue;
                if (hasFocus && !(focusSet.contains(e.fromId) && focusSet.contains(e.toId))) continue;
                boolean ehigh = hasSel && hl.contains(e.fromId) && hl.contains(e.toId);
                float alpha=1f;
                if (hasSel    && !ehigh)                                     alpha=0.09f;
                if (hasFilter && !matchesFilter(from) && !matchesFilter(to)) alpha=0.06f;
                DNode drawFrom = hasFocus ? withY(from, focusY.get(from.id)) : from;
                DNode drawTo   = hasFocus ? withY(to,   focusY.get(to.id))   : to;
                drawEdge(g, drawFrom, drawTo, ehigh, alpha);
            }

            // 4) Ombres + nœuds
            for (DNode n : model.nodes.values()) {
                if (hasFocus && !focusSet.contains(n.id)) continue;
                drawNodeShadow(g, hasFocus ? withY(n, focusY.get(n.id)) : n, nodeAlpha(n,hasSel,hasFilter,hl));
            }
            for (DNode n : model.nodes.values()) {
                if (hasFocus && !focusSet.contains(n.id)) continue;
                DNode draw = hasFocus ? withY(n, focusY.get(n.id)) : n;
                drawNode(g, draw, n.id.equals(selectedId), n.id.equals(hoveredId), nodeAlpha(n,hasSel,hasFilter,hl));
            }

            // 5) Tooltip
            if (hoveredId!=null) {
                DNode n=model.nodes.get(hoveredId);
                if (n!=null) {
                    DNode draw = (hasFocus && focusSet!=null && focusSet.contains(n.id))
                        ? withY(n, focusY.get(n.id)) : n;
                    drawTooltip(g, draw);
                }
            }

            g.dispose();
        }

        private float nodeAlpha(DNode n, boolean hasSel, boolean hasFilter, Set<String> hl) {
            boolean nhigh   = !hasSel    || hl.contains(n.id);
            boolean matches = !hasFilter || matchesFilter(n);
            float   alpha   = (nhigh && matches) ? 1f : 0.16f;
            if (hasSel && !nhigh) alpha = Math.min(alpha, 0.20f);
            return alpha;
        }

        // ── Zones de groupe ───────────────────────────────────────────────────

        /**
         * Calcule la bounding-box réelle d'un groupe à partir des nœuds visibles
         * et de leurs positions effectives (focus ou normales).
         * Retourne null si aucun nœud du groupe n'est visible.
         */
        private Rectangle2D computeGroupRect(NodeType[] members,
                                             Set<String> focusSet,
                                             Map<String,Double> focusY) {
            double xMin=Double.MAX_VALUE, xMax=Double.MIN_VALUE;
            double yMin=Double.MAX_VALUE, yMax=Double.MIN_VALUE;
            boolean found = false;
            for (NodeType t : members) {
                for (DNode n : model.nodes.values()) {
                    if (n.type != t) continue;
                    if (focusSet != null && !focusSet.contains(n.id)) continue;
                    double ny = (focusY != null && focusY.containsKey(n.id)) ? focusY.get(n.id) : n.y;
                    xMin = Math.min(xMin, n.x);
                    xMax = Math.max(xMax, n.x + NW);
                    yMin = Math.min(yMin, ny);
                    yMax = Math.max(yMax, ny + NH);
                    found = true;
                }
            }
            if (!found) return null;
            return new Rectangle2D.Double(
                xMin - GRP_PAD, yMin - GRP_PAD - 18,
                (xMax - xMin) + GRP_PAD * 2,
                (yMax - yMin) + GRP_PAD * 2 + 18);
        }

        private void drawGroupBackgrounds(Graphics2D g, Set<String> focusSet, Map<String,Double> focusY) {
            String[]   keys   = { "FILTER",     "IMPORT",     "EXPORT"     };
            Color[]    colors = { C_GRP_FILTER, C_GRP_IMPORT, C_GRP_EXPORT };
            String[]   labels = { "FILTRES",    "IMPORTS",    "EXPORTS"    };
            NodeType[][] members = {
                { NodeType.FILTER_RESOURCE, NodeType.FILTER_EVENT },
                { NodeType.IMPORT_RESOURCE, NodeType.IMPORT_EVENT },
                { NodeType.EXPORT_RESOURCE, NodeType.EXPORT_EVENT }
            };

            for (int i = 0; i < keys.length; i++) {
                Rectangle2D r = computeGroupRect(members[i], focusSet, focusY);
                if (r == null) continue;
                Color base = colors[i];

                g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 16));
                g.fillRoundRect((int)r.getX(),(int)r.getY(),(int)r.getWidth(),(int)r.getHeight(), 18, 18);

                g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 130));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                            1f, new float[]{6, 5}, 0));
                g.drawRoundRect((int)r.getX(),(int)r.getY(),(int)r.getWidth(),(int)r.getHeight(), 18, 18);
                g.setStroke(new BasicStroke(1f));

                drawGroupLabel(g, r, labels[i], base);
            }
        }

        private void drawGroupLabel(Graphics2D g, Rectangle2D r, String label, Color base) {
            Font f = new Font(Font.SANS_SERIF, Font.BOLD, 13);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(label) + 20;
            int th = fm.getHeight() + 6;
            int bx = (int)(r.getX() + (r.getWidth() - tw) / 2);
            int by = (int)(r.getY() - th / 2 - 1);
            g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 200));
            g.fillRoundRect(bx, by, tw, th, 8, 8);
            g.setColor(Color.WHITE);
            g.drawString(label, bx + 10, by + fm.getAscent() + 3);
        }

        // ── Fonds de colonnes individuelles ───────────────────────────────────

        private void drawColumnBackgrounds(Graphics2D g, Set<String> focusSet, Map<String,Double> focusY) {
            for (NodeType t : COL_ORDER) {
                // Calculer la bounding-box réelle des nœuds visibles de cette colonne
                double yMin=Double.MAX_VALUE, yMax=Double.MIN_VALUE;
                boolean found = false;
                for (DNode n : model.nodes.values()) {
                    if (n.type != t) continue;
                    if (focusSet != null && !focusSet.contains(n.id)) continue;
                    double ny = (focusY != null && focusY.containsKey(n.id)) ? focusY.get(n.id) : n.y;
                    yMin = Math.min(yMin, ny);
                    yMax = Math.max(yMax, ny + NH);
                    found = true;
                }
                if (!found) continue;

                // X fixe (colonnes ne bougent pas horizontalement)
                Rectangle2D colBound = model.colBounds.get(t);
                if (colBound == null) continue;
                double x = colBound.getX();
                double w = colBound.getWidth();
                Rectangle2D r = new Rectangle2D.Double(
                    x, yMin - COL_PAD - 20,
                    w, (yMax - yMin) + COL_PAD * 2 + 20);

                Color base = palette(t)[0];
                g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 18));
                g.fillRoundRect((int)r.getX(),(int)r.getY(),(int)r.getWidth(),(int)r.getHeight(), 10, 10);

                g.setColor(cColBorder());
                g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                            1f, new float[]{4,4}, 0));
                g.drawRoundRect((int)r.getX(),(int)r.getY(),(int)r.getWidth(),(int)r.getHeight(), 10, 10);
                g.setStroke(new BasicStroke(1f));

                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                String title = colTitle(t);
                FontMetrics fm = g.getFontMetrics();
                int tx2 = (int)(r.getX() + (r.getWidth()-fm.stringWidth(title))/2);
                int ty2 = (int)(r.getY() + fm.getAscent() + 3);
                g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 190));
                g.drawString(title, tx2, ty2);
            }
        }

        // ── Grille ────────────────────────────────────────────────────────────

        private void drawGrid(Graphics2D g) {
            g.setColor(cGrid());
            g.setStroke(new BasicStroke(0.5f));
            for (int x=0; x<getWidth();  x+=40) g.drawLine(x,0,x,getHeight());
            for (int y=0; y<getHeight(); y+=40) g.drawLine(0,y,getWidth(),y);
        }

        // ── Arête ─────────────────────────────────────────────────────────────

        private void drawEdge(Graphics2D g, DNode from, DNode to, boolean hl, float alpha) {
            double x1=from.x+NW, y1=from.y+NH/2.0, x2=to.x, y2=to.y+NH/2.0;
            if (from.x>=to.x) { x1=from.x; x2=to.x+NW; }
            double dx=x2-x1, cx1=x1+dx*0.40, cy1=y1, cx2=x1+dx*0.60, cy2=y2;
            Color edgeColor = hl ? C_EDGE_HL : cEdge();
            g.setColor(withAlpha(edgeColor, alpha));
            g.setStroke(new BasicStroke(hl?2.2f:1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path=new Path2D.Double();
            path.moveTo(x1,y1); path.curveTo(cx1,cy1,cx2,cy2,x2,y2);
            g.draw(path);
            if (alpha>0.12f) drawArrow(g,cx2,cy2,x2,y2,edgeColor,alpha);
        }

        private void drawArrow(Graphics2D g, double ax,double ay,double tx2,double ty2,Color c,float alpha) {
            double angle=Math.atan2(ty2-ay,tx2-ax), len=9.0, spread=Math.toRadians(23);
            Path2D a=new Path2D.Double();
            a.moveTo(tx2,ty2);
            a.lineTo(tx2-len*Math.cos(angle-spread), ty2-len*Math.sin(angle-spread));
            a.lineTo(tx2-len*Math.cos(angle+spread), ty2-len*Math.sin(angle+spread));
            a.closePath();
            g.setColor(withAlpha(c,alpha)); g.fill(a);
        }

        // ── Nœud ──────────────────────────────────────────────────────────────

        private void drawNodeShadow(Graphics2D g, DNode n, float alpha) {
            if (alpha<0.4f||scale<0.5) return;
            g.setColor(withAlpha(new Color(0,0,0,60), alpha));
            g.fillRoundRect((int)n.x+3,(int)n.y+3,NW,NH,ARC,ARC);
        }

        private void drawNode(Graphics2D g, DNode n, boolean selected, boolean hovered, float alpha) {
            Color[] cols=palette(n.type);
            Color bg=withAlpha(cols[0],alpha);
            Color border=withAlpha(selected?C_SEL_RING:(hovered?cols[1].brighter():cols[1]),alpha);

            g.setColor(bg);
            g.fillRoundRect((int)n.x,(int)n.y,NW,NH,ARC,ARC);

            if (alpha>0.5f) {
                GradientPaint gp=new GradientPaint((float)n.x,(float)n.y,withAlpha(Color.WHITE,alpha*0.13f),
                                                   (float)n.x,(float)(n.y+NH/2),withAlpha(Color.WHITE,0));
                g.setPaint(gp);
                g.fillRoundRect((int)n.x,(int)n.y,NW,NH/2,ARC,ARC);
                g.setPaint(null);
            }

            g.setColor(border);
            g.setStroke(new BasicStroke(selected?2.5f:(hovered?1.9f:1.3f)));
            g.drawRoundRect((int)n.x,(int)n.y,NW,NH,ARC,ARC);

            if (alpha>0.3f) {
                g.setColor(withAlpha(cols[1],Math.min(alpha+0.2f,1f)));
                g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine((int)n.x+4,(int)n.y+6,(int)n.x+4,(int)n.y+NH-6);
            }

            if (alpha>0.15f) {
                int fontSize=(scale<0.55)?10:14;
                g.setFont(new Font(Font.SANS_SERIF, selected?Font.BOLD:Font.PLAIN, fontSize));
                FontMetrics fm=g.getFontMetrics();
                String lbl=truncate(n.label,fm,NW-20);
                int lx=(int)n.x+14, ly=(int)n.y+(NH+fm.getAscent()-fm.getDescent())/2;
                if (alpha>0.6f) { g.setColor(withAlpha(new Color(0,0,0,90),alpha)); g.drawString(lbl,lx+1,ly+1); }
                g.setColor(withAlpha(cText(),alpha));
                g.drawString(lbl,lx,ly);
            }
        }

        // ── Tooltip ───────────────────────────────────────────────────────────

        private void drawTooltip(Graphics2D g, DNode n) {
            long connCount=model.edges.stream().filter(e->e.fromId.equals(n.id)||e.toId.equals(n.id)).count();
            String text=n.label+"  ["+typeLbl(n.type)+"]  —  "+connCount+" connexion(s)";
            Font f=new Font(Font.SANS_SERIF,Font.PLAIN,12);
            g.setFont(f);
            FontMetrics fm=g.getFontMetrics();
            int tw=fm.stringWidth(text)+20, th=fm.getHeight()+12;
            int px=(int)(n.x+NW/2.0-tw/2.0), py=(int)(n.y-th-10);
            g.setColor(cTooltipBg());      g.fillRoundRect(px,py,tw,th,7,7);
            g.setColor(cTooltipBorder());  g.setStroke(new BasicStroke(1f)); g.drawRoundRect(px,py,tw,th,7,7);
            g.setColor(cText());           g.drawString(text,px+10,py+fm.getAscent()+6);
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private Color withAlpha(Color c, float a) {
            int alpha=Math.max(0,Math.min(255,(int)(c.getAlpha()*a)));
            return new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
        }

        private Color[] palette(NodeType t) {
            return switch (t) {
                case DIMENSION       -> new Color[]{ C_DIM,      C_DIM_B      };
                case HIERARCHY       -> new Color[]{ C_HIER,     C_HIER_B     };
                case FILTER_RESOURCE -> new Color[]{ C_FILT_RES, C_FILT_RES_B };
                case FILTER_EVENT    -> new Color[]{ C_FILT_EVT, C_FILT_EVT_B };
                case IMPORT_RESOURCE -> new Color[]{ C_IMP_RES,  C_IMP_RES_B  };
                case IMPORT_EVENT    -> new Color[]{ C_IMP_EVT,  C_IMP_EVT_B  };
                case EXPORT_RESOURCE -> new Color[]{ C_EXP_RES,  C_EXP_RES_B  };
                case EXPORT_EVENT    -> new Color[]{ C_EXP_EVT,  C_EXP_EVT_B  };
            };
        }

        private String typeLbl(NodeType t) {
            return switch (t) {
                case DIMENSION       -> "Dimension";
                case HIERARCHY       -> "Hiérarchie";
                case FILTER_RESOURCE -> "Filtre Ressource";
                case FILTER_EVENT    -> "Filtre Événement";
                case IMPORT_RESOURCE -> "Import Ressource";
                case IMPORT_EVENT    -> "Import Événement";
                case EXPORT_RESOURCE -> "Export Ressource";
                case EXPORT_EVENT    -> "Export Événement";
            };
        }

        private String colTitle(NodeType t) {
            return switch (t) {
                case DIMENSION       -> "RESSOURCES";
                case HIERARCHY       -> "HIÉRARCHIES";
                case FILTER_RESOURCE -> "FILTRE RES.";
                case FILTER_EVENT    -> "FILTRE ÉVT.";
                case IMPORT_RESOURCE -> "IMPORT RES.";
                case IMPORT_EVENT    -> "IMPORT ÉVT.";
                case EXPORT_RESOURCE -> "EXPORT RES.";
                case EXPORT_EVENT    -> "EXPORT ÉVT.";
            };
        }

        private String truncate(String s, FontMetrics fm, int maxW) {
            if (fm.stringWidth(s)<=maxW) return s;
            while (s.length()>1 && fm.stringWidth(s+"…")>maxW) s=s.substring(0,s.length()-1);
            return s+"…";
        }

        private Set<String> highlighted() {
            String focus = selectedId!=null ? selectedId : hoveredId;
            if (focus==null) return Collections.emptySet();
            return buildNeighbours(focus);
        }

        private Set<String> buildNeighbours(String id) {
            Set<String> ids=new HashSet<>(); ids.add(id);
            for (DEdge e : model.edges) {
                if (e.fromId.equals(id)) ids.add(e.toId);
                if (e.toId.equals(id))   ids.add(e.fromId);
            }
            return ids;
        }

        private boolean matchesFilter(DNode n) {
            return filterText.isEmpty() || n.label.toLowerCase(Locale.ROOT).contains(filterText);
        }

        private Point2D worldPt(Point p) { return new Point2D.Double((p.x-tx)/scale,(p.y-ty)/scale); }

        private String hitTest(Point2D w) {
            Set<String> focusSet = focusedId!=null ? buildNeighbours(focusedId) : null;
            Map<String,Double> focusY = computeFocusPositions(focusSet);
            for (DNode n : model.nodes.values()) {
                if (focusSet!=null && !focusSet.contains(n.id)) continue;
                double ny = (focusY!=null && focusY.containsKey(n.id)) ? focusY.get(n.id) : n.y;
                if (w.getX()>=n.x && w.getX()<=n.x+NW && w.getY()>=ny && w.getY()<=ny+NH) return n.id;
            }
            return null;
        }

        private void onPress(MouseEvent e)  {
            if (SwingUtilities.isRightMouseButton(e)) return; // pas de drag sur clic droit
            dragPt=e.getPoint(); dragTX=tx; dragTY=ty;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        private void onDrag(MouseEvent e)   { if(dragPt==null)return; tx=dragTX+(e.getX()-dragPt.x); ty=dragTY+(e.getY()-dragPt.y); repaint(); }

        private void onClick(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) { onRightClick(e); return; }

            String hit=hitTest(worldPt(e.getPoint()));

            // Clic gauche sur nœud focalisé ou sur le vide → désélectionner et sortir du mode focus
            if (focusedId!=null && (hit==null || hit.equals(focusedId))) {
                focusedId=null; selectedId=null;
                if (infoLabel!=null) infoLabel.setText(" ");
                repaint(); return;
            }

            selectedId=Objects.equals(hit,selectedId)?null:hit;
            if (infoLabel!=null) {
                if (selectedId!=null) {
                    DNode n=model.nodes.get(selectedId);
                    long  c=model.edges.stream().filter(ed->ed.fromId.equals(selectedId)||ed.toId.equals(selectedId)).count();
                    infoLabel.setText("  "+n.label+"  —  "+typeLbl(n.type)+"  ·  "+c+" connexion(s)");
                } else { infoLabel.setText(" "); }
            }
            repaint();
        }

        private void onRightClick(MouseEvent e) {
            String hit=hitTest(worldPt(e.getPoint()));
            if (hit==null) {
                // Clic droit sur le vide : sortir du mode focus si actif
                if (focusedId!=null) { focusedId=null; selectedId=null; if(infoLabel!=null) infoLabel.setText(" "); repaint(); }
                return;
            }

            DNode n=model.nodes.get(hit);
            if (n==null) return;

            JPopupMenu menu = new JPopupMenu();

            // Titre non-cliquable
            JMenuItem title = new JMenuItem(n.label);
            title.setEnabled(false);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
            menu.add(title);
            menu.addSeparator();

            boolean isFocused = hit.equals(focusedId);
            if (!isFocused) {
                long connCount = model.edges.stream()
                    .filter(ed->ed.fromId.equals(hit)||ed.toId.equals(hit)).count();
                JMenuItem showOnly = new JMenuItem("Afficher uniquement les connexions  (" + connCount + ")");
                showOnly.addActionListener(ae -> {
                    focusedId  = hit;
                    selectedId = hit;
                    if (infoLabel!=null) {
                        infoLabel.setText("  "+n.label+"  —  "+typeLbl(n.type)
                            +"  ·  "+connCount+" connexion(s)  [mode focus]");
                    }
                    repaint();
                });
                menu.add(showOnly);
            } else {
                JMenuItem showAll = new JMenuItem("Réafficher tout");
                showAll.addActionListener(ae -> {
                    focusedId=null; selectedId=null;
                    if (infoLabel!=null) infoLabel.setText(" ");
                    repaint();
                });
                menu.add(showAll);
            }

            menu.addSeparator();
            JMenuItem infoItem = new JMenuItem("Infos…");
            infoItem.addActionListener(ae -> showNodeInfo(n));
            menu.add(infoItem);

            menu.show(this, e.getX(), e.getY());
        }

        // ── Fenêtre d'infos ───────────────────────────────────────────────────

        private void showNodeInfo(DNode n) {
            if (n.entity == null) return;
            new NodeInfoDialog(SwingUtilities.getWindowAncestor(this), n).setVisible(true);
        }

        private void onMove(MouseEvent e) {
            String hit=hitTest(worldPt(e.getPoint()));
            if (!Objects.equals(hit,hoveredId)) {
                hoveredId=hit;
                setCursor(hit!=null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                repaint();
            }
        }

        private void onWheel(MouseWheelEvent e) {
            double factor=(e.getWheelRotation()<0)?1.12:0.90;
            double newScale=Math.max(0.10,Math.min(5.0,scale*factor));
            double mx=e.getX(), my=e.getY();
            tx=mx-(mx-tx)*(newScale/scale);
            ty=my-(my-ty)*(newScale/scale);
            scale=newScale;
            repaint();
        }
    }

    // =========================================================================
    // Fenêtre d'informations d'un nœud
    // =========================================================================

    /**
     * Dialog non-modale affichant les Parameters et leurs attributs
     * de l'Entity associée au nœud du diagramme.
     */
    class NodeInfoDialog extends JDialog {

        private static final Color BG        = new Color(245, 247, 251);
        private static final Color BG_CARD   = Color.WHITE;
        private static final Color BG_HEADER = new Color(235, 239, 248);
        private static final Color FG_TITLE  = new Color(20,  30,  55);
        private static final Color FG_DIM    = new Color(70,  85, 120);
        private static final Color FG_VALUE  = new Color(35,  42,  58);
        private static final Color BORDER    = new Color(210, 218, 232);
        private static final Color ACCENT_BG = new Color(230, 238, 255);
        private static final Color ACCENT_FG = new Color(40,  80, 190);

        NodeInfoDialog(java.awt.Window parent, DNode n) {
            super(parent, "Infos — " + n.label, ModalityType.MODELESS);
            setSize(520, 580);
            setLocationRelativeTo(parent);
            setResizable(true);

            JPanel root = new JPanel(new BorderLayout(0, 0));
            root.setBackground(BG);

            // ── En-tête ──────────────────────────────────────────────────────
            JPanel header = new JPanel(new BorderLayout(10, 0));
            header.setBackground(BG_HEADER);
            header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(12, 16, 12, 16)));

            Color[] pal = diagramPalette(n.type);
            JLabel badge = new JLabel(diagramTypeLbl(n.type));
            badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            badge.setForeground(pal[0].darker());
            badge.setOpaque(true);
            badge.setBackground(new Color(pal[0].getRed(), pal[0].getGreen(), pal[0].getBlue(), 30));
            badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(pal[0].getRed(), pal[0].getGreen(), pal[0].getBlue(), 80), 1),
                new EmptyBorder(2, 8, 2, 8)));

            JLabel nameLabel = new JLabel(n.label);
            nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            nameLabel.setForeground(FG_TITLE);

            JPanel headerLeft = new JPanel();
            headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
            headerLeft.setOpaque(false);
            badge.setAlignmentX(Component.LEFT_ALIGNMENT);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerLeft.add(badge);
            headerLeft.add(Box.createVerticalStrut(5));
            headerLeft.add(nameLabel);
            header.add(headerLeft, BorderLayout.CENTER);
            root.add(header, BorderLayout.NORTH);

            // ── Contenu scrollable ────────────────────────────────────────────
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(BG);
            content.setBorder(new EmptyBorder(10, 12, 10, 12));

            if (n.entity == null || n.entity.getParameters().isEmpty()) {
                JLabel empty = new JLabel("Aucun paramètre disponible.");
                empty.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
                empty.setForeground(FG_DIM);
                content.add(empty);
            } else {
                for (Parameters param : n.entity.getParameters()) {
                    content.add(buildParamCard(param));
                    content.add(Box.createVerticalStrut(8));
                }
            }
            content.add(Box.createVerticalGlue());

            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(BG);
            scroll.getVerticalScrollBar().setUnitIncrement(14);
            root.add(scroll, BorderLayout.CENTER);

            // ── Pied ─────────────────────────────────────────────────────────
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
            footer.setBackground(BG_HEADER);
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
            JButton closeBtn = new JButton("Fermer");
            closeBtn.putClientProperty("JButton.buttonType", "roundRect");
            closeBtn.addActionListener(e -> dispose());
            footer.add(closeBtn);
            root.add(footer, BorderLayout.SOUTH);

            setContentPane(root);
        }

        private JPanel buildParamCard(Parameters param) {
            JPanel card = new JPanel(new BorderLayout(0, 0));
            card.setBackground(BG_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(0, 0, 4, 0)));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            // Titre du Parameters
            JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
            titleBar.setBackground(ACCENT_BG);
            titleBar.setBorder(BorderFactory.createMatteBorder(0, 3, 1, 0, ACCENT_FG));
            JLabel titleLbl = new JLabel(param.getName());
            titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            titleLbl.setForeground(ACCENT_FG);
            titleBar.add(titleLbl);
            card.add(titleBar, BorderLayout.NORTH);

            // Attributs
            List<com.stilog.prism.vpimodel.objects.Attribute> attrs = param.getAttributes();
            if (attrs.isEmpty()) {
                JLabel noAttr = new JLabel("  (aucun attribut)");
                noAttr.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
                noAttr.setForeground(FG_DIM);
                noAttr.setBorder(new EmptyBorder(4, 8, 4, 8));
                card.add(noAttr, BorderLayout.CENTER);
            } else {
                JPanel attrPanel = new JPanel();
                attrPanel.setLayout(new BoxLayout(attrPanel, BoxLayout.Y_AXIS));
                attrPanel.setBackground(BG_CARD);
                attrPanel.setBorder(new EmptyBorder(4, 8, 4, 8));

                for (com.stilog.prism.vpimodel.objects.Attribute attr : attrs) {
                    // Ignorer les attributs internes __DIM
                    if (attr.getKey().endsWith(VPIConstants.CORRESPONDANCE_DIM_SUFFIX)) continue;

                    JPanel row = new JPanel(new BorderLayout(12, 0));
                    row.setOpaque(false);
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                    row.setBorder(new EmptyBorder(1, 0, 1, 0));

                    JLabel keyLbl = new JLabel(attr.getKey());
                    keyLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    keyLbl.setForeground(FG_DIM);
                    keyLbl.setPreferredSize(new Dimension(180, 20));

                    String val = attr.getValue();
                    JLabel valLbl = new JLabel(val == null || val.isBlank() ? "—" : val);
                    valLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                    valLbl.setForeground(FG_VALUE);

                    row.add(keyLbl, BorderLayout.WEST);
                    row.add(valLbl, BorderLayout.CENTER);
                    attrPanel.add(row);
                }
                card.add(attrPanel, BorderLayout.CENTER);
            }
            return card;
        }

        // Délégation vers les méthodes de VPIDependencyDiagramTab
        private Color[] diagramPalette(NodeType t) {
            return switch (t) {
                case DIMENSION       -> new Color[]{ C_DIM,      C_DIM_B      };
                case HIERARCHY       -> new Color[]{ C_HIER,     C_HIER_B     };
                case FILTER_RESOURCE -> new Color[]{ C_FILT_RES, C_FILT_RES_B };
                case FILTER_EVENT    -> new Color[]{ C_FILT_EVT, C_FILT_EVT_B };
                case IMPORT_RESOURCE -> new Color[]{ C_IMP_RES,  C_IMP_RES_B  };
                case IMPORT_EVENT    -> new Color[]{ C_IMP_EVT,  C_IMP_EVT_B  };
                case EXPORT_RESOURCE -> new Color[]{ C_EXP_RES,  C_EXP_RES_B  };
                case EXPORT_EVENT    -> new Color[]{ C_EXP_EVT,  C_EXP_EVT_B  };
            };
        }

        private String diagramTypeLbl(NodeType t) {
            return switch (t) {
                case DIMENSION       -> "Dimension";
                case HIERARCHY       -> "Hiérarchie";
                case FILTER_RESOURCE -> "Filtre Ressource";
                case FILTER_EVENT    -> "Filtre Événement";
                case IMPORT_RESOURCE -> "Import Ressource";
                case IMPORT_EVENT    -> "Import Événement";
                case EXPORT_RESOURCE -> "Export Ressource";
                case EXPORT_EVENT    -> "Export Événement";
            };
        }
    }
}
