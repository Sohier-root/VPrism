package com.stilog.analyzemodule.view.tabs;

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
import com.stilog.analysevpi.model.GeneralCorrespondance;
import com.stilog.analysevpi.utils.VPIConstants;
import com.stilog.view.ThemeManager;
import com.stilog.vpimodel.objects.Entity;
import com.stilog.vpimodel.objects.Parameters;
import com.stilog.vpimodel.objects.TypeData;
import com.stilog.vpimodel.objects.VPIDatas;
import com.stilog.vpimodel.vpsettings.FileDatas;

/**
 * Onglet 2 du Module 2 : diagramme de dépendances des dimensions.
 *
 * Layout en couches (colonnes par type) avec heuristique barycentre pour
 * minimiser les croisements d'arêtes.
 *
 * Interactions :
 *  - Pan : clic-glisser
 *  - Zoom : molette (centré sur curseur)
 *  - Sélection : clic sur un nœud (met en évidence ses voisins)
 *  - Recherche : barre en haut
 *  - Reset vue : bouton dédié
 */
public class VPIDependencyDiagramTab implements AbstractVPITab {

    private static final String TAB_TITLE = "Dépendances";

    // ── Palette — lue dynamiquement depuis ThemeManager ───────────────────────
    private final ThemeManager theme = ThemeManager.getInstance();

    // Couleurs des nœuds (identiques dark/light — assez saturées pour les deux)
    private static final Color C_DIM         = new Color(52, 120, 215);
    private static final Color C_DIM_B       = new Color(90, 160, 255);
    private static final Color C_HIER        = new Color(34, 160, 100);
    private static final Color C_HIER_B      = new Color(70, 210, 140);
    private static final Color C_FILT        = new Color(200, 100, 30);
    private static final Color C_FILT_B      = new Color(240, 150, 60);
    private static final Color C_IE          = new Color(130, 50, 190);
    private static final Color C_IE_B        = new Color(180, 100, 240);
    private static final Color C_EDGE_HL     = new Color(255, 200, 60, 230);
    private static final Color C_SEL_RING    = new Color(255, 220, 60);

    // Couleurs thème-dépendantes via méthodes
    private Color cBg()          { return theme.isDark() ? new Color(28, 32, 42)    : new Color(235, 238, 245); }
    private Color cGrid()        { return theme.isDark() ? new Color(40, 46, 60)    : new Color(215, 220, 232); }
    private Color cEdge()        { return theme.isDark() ? new Color(100, 110, 130, 160) : new Color(80, 90, 120, 130); }
    private Color cText()        { return theme.isDark() ? Color.WHITE               : new Color(15, 20, 35); }
    private Color cTextDim()     { return theme.isDark() ? new Color(180, 190, 210)  : new Color(70, 80, 110); }
    private Color cBarBg()       { return theme.isDark() ? new Color(36, 41, 55)     : new Color(255, 255, 255); }
    private Color cFieldBg()     { return theme.isDark() ? new Color(50, 56, 72)     : new Color(245, 247, 252); }
    private Color cFieldBorder() { return theme.isDark() ? new Color(80, 90, 120)    : new Color(195, 202, 218); }
    private Color cTooltipBg()   { return theme.isDark() ? new Color(18, 22, 34, 225): new Color(30, 35, 50, 230); }
    private Color cTooltipBorder(){ return theme.isDark()? new Color(80, 90, 130)    : new Color(150, 160, 190); }

    // ── Géométrie ─────────────────────────────────────────────────────────────
    private static final int NW      = 152;
    private static final int NH      = 34;
    private static final int ARC     = 8;
    private static final int COL_GAP = 230;
    private static final int ROW_GAP = 16;

    // ── UI ────────────────────────────────────────────────────────────────────
    private final JPanel  panel;
    private DiagramCanvas canvas;
    private JTextField    searchField;
    private JLabel        infoLabel;
    private DiagramModel  lastModel; // conservé pour rebuilder au changement de thème

    public VPIDependencyDiagramTab() {
        panel = new JPanel(new BorderLayout());
        panel.setBackground(cBg());
        showPlaceholder();

        // Rebuilder le diagramme au changement de thème
        theme.addChangeListener(() -> {
            panel.setBackground(cBg());
            if (lastModel != null) rebuildUI();
            else {
                panel.repaint();
                // Mettre à jour le label du placeholder
                panel.removeAll();
                showPlaceholder();
                panel.revalidate();
                panel.repaint();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AbstractVPITab
    // ─────────────────────────────────────────────────────────────────────────

    @Override public String getTabTitle() { return TAB_TITLE; }
    @Override public JPanel getPanel()    { return panel; }

    @Override
    public void onDataLoaded(VPIDatas data) {
        lastModel = buildModel(data);
        layoutColumns(lastModel);
        rebuildUI();
    }

    private void rebuildUI() {
        // Préserver l'état de recherche si possible
        String currentSearch = (searchField != null) ? searchField.getText() : "";

        panel.removeAll();
        panel.setBackground(cBg());
        panel.add(buildTopBar(),           BorderLayout.NORTH);
        canvas = new DiagramCanvas(lastModel);
        panel.add(canvas,                  BorderLayout.CENTER);
        panel.add(buildLegend(),           BorderLayout.SOUTH);

        if (!currentSearch.isEmpty() && searchField != null) {
            searchField.setText(currentSearch);
        }
        panel.revalidate();
        panel.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Placeholder
    // ─────────────────────────────────────────────────────────────────────────

    private void showPlaceholder() {
        JLabel lbl = new JLabel(
            "Importez un VPI/VPS dans l'onglet « Arbre » pour afficher le diagramme.",
            SwingConstants.CENTER);
        lbl.setForeground(cTextDim());
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC, 14f));
        panel.add(lbl, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Barre de recherche & contrôles
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Légende
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildLegend() {
        JPanel leg = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        leg.setBackground(cBarBg());
        leg.setBorder(new EmptyBorder(4, 8, 4, 8));
        addLegendItem(leg, C_DIM,  "Dimension");
        addLegendItem(leg, C_HIER, "Hiérarchie");
        addLegendItem(leg, C_FILT, "Filtre");
        addLegendItem(leg, C_IE,   "Import / Export");
        JLabel hint = new JLabel("  |  Clic : sélectionner  ·  Molette : zoom  ·  Glisser : pan");
        hint.setForeground(cTextDim());
        hint.setFont(hint.getFont().deriveFont(11f));
        leg.add(hint);
        return leg;
    }

    private void addLegendItem(JPanel p, Color c, String label) {
        final Color cc = c;
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cc);
                g2.fillRoundRect(0, (getHeight()-14)/2, 20, 14, 4, 4);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(22, 16); }
        };
        dot.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(cTextDim());
        lbl.setFont(lbl.getFont().deriveFont(12f));
        JPanel entry = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        entry.setOpaque(false);
        entry.add(dot);
        entry.add(lbl);
        p.add(entry);
    }

    // =========================================================================
    // Modèle de données
    // =========================================================================

    enum NodeType { DIMENSION, HIERARCHY, FILTER, IMPORT_EXPORT }

    static class DNode {
        final String   id, label;
        final NodeType type;
        double x, y;
        int    rank;   // ordre vertical dans sa colonne

        DNode(String id, String label, NodeType type) {
            this.id = id; this.label = label; this.type = type;
        }
    }

    static class DEdge {
        final String fromId, toId;
        DEdge(String f, String t) { fromId = f; toId = t; }
    }

    static class DiagramModel {
        final Map<String, DNode> nodes = new LinkedHashMap<>();
        final List<DEdge>        edges = new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Construction du modèle
    // ─────────────────────────────────────────────────────────────────────────

    private DiagramModel buildModel(VPIDatas data) {
        DiagramModel m = new DiagramModel();

        // Passe 1 : créer tous les noeuds
        for (FileDatas fd : data.getFilesDatas()) {
            if (fd == null) continue;
            for (Entity entity : fd.getEntities()) {
                NodeType nt  = resolveType(entity, fd.getName());
                String   nid = nt.name() + "::" + entity.getName();
                m.nodes.putIfAbsent(nid, new DNode(nid, entity.getName(), nt));
            }
        }

        // Passe 2 : aretes selon la structure propre a chaque type
        for (FileDatas fd : data.getFilesDatas()) {
            if (fd == null) continue;
            String fdName = fd.getName();

            for (Entity entity : fd.getEntities()) {
                NodeType nt  = resolveType(entity, fdName);
                String   nid = nt.name() + "::" + entity.getName();

                if (nt == NodeType.DIMENSION) {
                    // Dimension -> Dimension : attributs de type ResourceReference
                    // param.getAttributeValue(PARAMETER_RESOURCEMODEL) contient le nom
                    // de la dimension referencee (resolu par ResourceModel.computeHeadings)
                    for (Parameters param : entity.getParameters()) {
                        String refDim = param.getAttributeValue(VPIConstants.PARAMETER_RESOURCEMODEL);
                        if (refDim != null && !refDim.isBlank())
                            linkToDimension(m, nid, refDim);
                    }

                } else if (nt == NodeType.HIERARCHY) {
                    // Hierarchie -> Dimension (1) : param.getName() == nom de la dimension
                    // Hierarchie -> Dimension (2) : filtre inline dans PARAMETER_CONDITIONS
                    for (Parameters param : entity.getParameters()) {
                        linkToDimension(m, nid, param.getName());
                        String condXml = param.getAttributeValue(VPIConstants.PARAMETER_CONDITIONS);
                        if (condXml != null && !condXml.isBlank()) {
                            for (String dimId : extractDimIdsFromConditionXml(condXml)) {
                                String dimName = GeneralCorrespondance.getInstance()
                                    .getCorrespondance(VPIConstants.PARAMETER_RESOURCEMODEL, dimId);
                                if (dimName != null && !dimName.isBlank())
                                    linkToDimension(m, nid, dimName);
                            }
                        }
                    }

                } else if (nt == NodeType.IMPORT_EXPORT) {
                    // Import/Export -> Dimension : attribut PARAMETER_RESOURCEMODEL
                    // dans le Parameters nomme PARAMETER_PARAMETRE
                    for (Parameters param : entity.getParameters()) {
                        if (VPIConstants.PARAMETER_PARAMETRE.equals(param.getName())) {
                            String dimName = param.getAttributeValue(VPIConstants.PARAMETER_RESOURCEMODEL);
                            if (dimName != null && !dimName.isBlank())
                                linkToDimension(m, nid, dimName);
                        }
                    }

                } else if (nt == NodeType.FILTER) {
                    // Filtre -> Dimension : le XML des conditions contient
                    // <resourceModel><entityID>ID</entityID></resourceModel>
                    // L'ID est resolvable via GeneralCorrespondance(PARAMETER_RESOURCEMODEL)
                    for (Parameters param : entity.getParameters()) {
                        String condXml = param.getAttributeValue(VPIConstants.PARAMETER_CONDITIONS);
                        if (condXml != null && !condXml.isBlank()) {
                            extractDimIdsFromConditionXml(condXml).forEach(dimId -> {
                                String dimName = GeneralCorrespondance.getInstance()
                                    .getCorrespondance(VPIConstants.PARAMETER_RESOURCEMODEL, dimId);
                                if (dimName != null && !dimName.isBlank())
                                    linkToDimension(m, nid, dimName);
                            });
                        }
                    }
                }
            }
        }
        return m;
    }

    /**
     * Parse le XML brut des conditions d'un filtre et retourne les entityID
     * distincts trouves dans les balises <resourceModel><entityID>.
     *
     * Structure attendue (EventResourceFilter / PlanningEventFilter) :
     * <resourceModel><entityID>2</entityID></resourceModel>
     */
    private Set<String> extractDimIdsFromConditionXml(String xml) {
        Set<String> ids = new LinkedHashSet<>();
        try {
            // Le XML stocke peut etre un fragment (pas de racine unique) ;
            // on l'enveloppe pour le rendre parseable.
            String wrapped = "<root>" + xml + "</root>";
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(wrapped)));
            NodeList resourceModels = doc.getElementsByTagName("resourceModel");
            for (int i = 0; i < resourceModels.getLength(); i++) {
                NodeList children = resourceModels.item(i).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if ("entityID".equals(children.item(j).getNodeName())) {
                        String id = children.item(j).getTextContent().trim();
                        if (!id.isBlank() && !"-1".equals(id))
                            ids.add(id);
                    }
                }
            }
        } catch (Exception e) {
            // XML malformé ou fragment imprevu : on ignore silencieusement
        }
        return ids;
    }

    /**
     * Cree une arete de fromId vers la dimension nommee dimName si elle existe.
     */
    private void linkToDimension(DiagramModel m, String fromId, String dimName) {
        if (dimName == null || dimName.isBlank()) return;
        String dimId = NodeType.DIMENSION.name() + "::" + dimName;
        if (m.nodes.containsKey(dimId) && !fromId.equals(dimId))
            addEdgeIfAbsent(m, fromId, dimId);
    }

    private void addEdgeIfAbsent(DiagramModel m, String from, String to) {
        for (DEdge e : m.edges)
            if (e.fromId.equals(from) && e.toId.equals(to)) return;
        m.edges.add(new DEdge(from, to));
    }

    private NodeType resolveType(Entity entity, String fdName) {
        if (entity.getTypeData() == TypeData.DIMENSION)                return NodeType.DIMENSION;
        if (fdName.equals(VPIConstants.NAME_TREE_EVENTSSTRUCT))        return NodeType.HIERARCHY;
        if (fdName.equals(VPIConstants.NAME_TREE_RESOURCESFILTER)
         || fdName.equals(VPIConstants.NAME_TREE_EVENTSFILTER))        return NodeType.FILTER;
        if (fdName.equals(VPIConstants.NAME_TREE_RESOURCESEXPORT)
         || fdName.equals(VPIConstants.NAME_TREE_EVENTSEXPORT)
         || fdName.equals(VPIConstants.NAME_TREE_RESOURCESIMPORT)
         || fdName.equals(VPIConstants.NAME_TREE_EVENTSIMPORT))        return NodeType.IMPORT_EXPORT;
        return NodeType.DIMENSION;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout en couches (Sugiyama simplifié avec heuristique barycentre)
    // ─────────────────────────────────────────────────────────────────────────

    private void layoutColumns(DiagramModel m) {
        NodeType[] colOrder = {
            NodeType.DIMENSION, NodeType.HIERARCHY,
            NodeType.FILTER,    NodeType.IMPORT_EXPORT
        };

        // Grouper
        Map<NodeType, List<DNode>> cols = new LinkedHashMap<>();
        for (NodeType t : colOrder) cols.put(t, new ArrayList<>());
        for (DNode n : m.nodes.values()) cols.get(n.type).add(n);

        // Tri alpha initial + affectation rang
        for (List<DNode> list : cols.values()) {
            list.sort(Comparator.comparing(n -> n.label.toLowerCase(Locale.ROOT)));
            for (int i = 0; i < list.size(); i++) list.get(i).rank = i;
        }

        // Heuristique barycentre (3 passes)
        for (int pass = 0; pass < 3; pass++) {
            for (int c = 1; c < colOrder.length; c++) {
                List<DNode> cur  = cols.get(colOrder[c]);
                List<DNode> prev = cols.get(colOrder[c - 1]);
                Map<String, Integer> prevRank = new HashMap<>();
                for (DNode n : prev) prevRank.put(n.id, n.rank);

                Map<String, Double> bary = new HashMap<>();
                for (DNode n : cur) {
                    List<Integer> ranks = new ArrayList<>();
                    for (DEdge e : m.edges) {
                        if (e.fromId.equals(n.id) && prevRank.containsKey(e.toId))
                            ranks.add(prevRank.get(e.toId));
                        if (e.toId.equals(n.id)   && prevRank.containsKey(e.fromId))
                            ranks.add(prevRank.get(e.fromId));
                    }
                    bary.put(n.id, ranks.isEmpty()
                        ? (double) n.rank
                        : ranks.stream().mapToInt(i -> i).average().orElse(n.rank));
                }
                cur.sort(Comparator.comparingDouble(n -> bary.getOrDefault(n.id, (double) n.rank)));
                for (int i = 0; i < cur.size(); i++) cur.get(i).rank = i;
            }
        }

        // Coordonnées finales
        int maxRows = cols.values().stream().mapToInt(List::size).max().orElse(1);
        int totalH  = maxRows * (NH + ROW_GAP);

        int colIdx = 0;
        for (NodeType t : colOrder) {
            List<DNode> list = cols.get(t);
            if (!list.isEmpty()) {
                int x     = 40 + colIdx * COL_GAP;
                int colH  = list.size() * (NH + ROW_GAP) - ROW_GAP;
                int startY = 40 + (totalH - colH) / 2;
                for (DNode n : list) {
                    n.x = x;
                    n.y = startY + n.rank * (NH + ROW_GAP);
                }
            }
            colIdx++;
        }
    }

    // =========================================================================
    // Canvas interactif
    // =========================================================================

    class DiagramCanvas extends JPanel {

        private final DiagramModel model;

        // Navigation
        private double scale = 1.0;
        private double tx = 40, ty = 30;
        private Point  dragPt;
        private double dragTX, dragTY;

        // État
        private String selectedId   = null;
        private String hoveredId    = null;
        private String filterText   = "";

        DiagramCanvas(DiagramModel model) {
            this.model = model;
            setBackground(cBg());

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)  { onPress(e); }
                @Override public void mouseDragged(MouseEvent e)  { onDrag(e); }
                @Override public void mouseReleased(MouseEvent e) { setCursor(Cursor.getDefaultCursor()); }
                @Override public void mouseClicked(MouseEvent e)  { onClick(e); }
                @Override public void mouseMoved(MouseEvent e)    { onMove(e); }
                @Override public void mouseWheelMoved(MouseWheelEvent e) { onWheel(e); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
        }

        void setFilter(String f) { filterText = f; selectedId = null; repaint(); }

        void resetView() {
            scale = 1.0; tx = 40; ty = 30;
            repaint();
        }

        // ── Rendu ──────────────────────────────────────────────────────────

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

            Set<String> hl = highlighted();
            boolean hasSel    = selectedId != null;
            boolean hasFilter = !filterText.isEmpty();

            // Arêtes
            for (DEdge e : model.edges) {
                DNode from = model.nodes.get(e.fromId);
                DNode to   = model.nodes.get(e.toId);
                if (from == null || to == null) continue;

                boolean ehigh = hasSel && hl.contains(e.fromId) && hl.contains(e.toId);
                float   alpha = 1f;
                if (hasSel    && !ehigh)                             alpha = 0.10f;
                if (hasFilter && !matchesFilter(from) && !matchesFilter(to)) alpha = 0.06f;

                drawEdge(g, from, to, ehigh, alpha);
            }

            // Nœuds (ombres d'abord)
            for (DNode n : model.nodes.values()) {
                boolean nhigh   = !hasSel || hl.contains(n.id);
                boolean matches = !hasFilter || matchesFilter(n);
                float   alpha   = (nhigh && matches) ? 1f : 0.18f;
                if (hasSel && !nhigh) alpha = Math.min(alpha, 0.22f);

                drawNodeShadow(g, n, alpha);
            }

            // Nœuds
            for (DNode n : model.nodes.values()) {
                boolean nhigh   = !hasSel || hl.contains(n.id);
                boolean matches = !hasFilter || matchesFilter(n);
                float   alpha   = (nhigh && matches) ? 1f : 0.18f;
                if (hasSel && !nhigh) alpha = Math.min(alpha, 0.22f);

                drawNode(g, n, n.id.equals(selectedId), n.id.equals(hoveredId), alpha);
            }

            // Tooltip
            if (hoveredId != null) {
                DNode n = model.nodes.get(hoveredId);
                if (n != null) drawTooltip(g, n);
            }

            g.dispose();
        }

        // ── Grille ──────────────────────────────────────────────────────────

        private void drawGrid(Graphics2D g) {
            g.setColor(cGrid());
            g.setStroke(new BasicStroke(0.5f));
            int step = 40;
            for (int x = 0; x < getWidth(); x += step)  g.drawLine(x, 0, x, getHeight());
            for (int y = 0; y < getHeight(); y += step) g.drawLine(0, y, getWidth(), y);
        }

        // ── Arête ───────────────────────────────────────────────────────────

        private void drawEdge(Graphics2D g, DNode from, DNode to, boolean hl, float alpha) {
            // Sortie côté droit du nœud source, entrée côté gauche du nœud cible
            double x1 = from.x + NW,      y1 = from.y + NH / 2.0;
            double x2 = to.x,             y2 = to.y   + NH / 2.0;

            // Arête "inverse" (même colonne ou colonne à gauche)
            if (from.x >= to.x) {
                x1 = from.x; x2 = to.x + NW;
            }

            double dx = x2 - x1;
            double cx1 = x1 + dx * 0.40, cy1 = y1;
            double cx2 = x1 + dx * 0.60, cy2 = y2;

            Color edgeColor = hl ? C_EDGE_HL : cEdge();
            g.setColor(withAlpha(edgeColor, alpha));
            g.setStroke(new BasicStroke(hl ? 2f : 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D path = new Path2D.Double();
            path.moveTo(x1, y1);
            path.curveTo(cx1, cy1, cx2, cy2, x2, y2);
            g.draw(path);

            // Pointe de flèche
            if (alpha > 0.12f)
                drawArrow(g, cx2, cy2, x2, y2, edgeColor, alpha);
        }

        private void drawArrow(Graphics2D g, double ax, double ay,
                               double tx, double ty, Color c, float alpha) {
            double angle  = Math.atan2(ty - ay, tx - ax);
            double len    = 8.0;
            double spread = Math.toRadians(24);
            double x1 = tx - len * Math.cos(angle - spread);
            double y1 = ty - len * Math.sin(angle - spread);
            double x2 = tx - len * Math.cos(angle + spread);
            double y2 = ty - len * Math.sin(angle + spread);
            Path2D a = new Path2D.Double();
            a.moveTo(tx, ty); a.lineTo(x1, y1); a.lineTo(x2, y2); a.closePath();
            g.setColor(withAlpha(c, alpha));
            g.fill(a);
        }

        // ── Nœud ────────────────────────────────────────────────────────────

        private void drawNodeShadow(Graphics2D g, DNode n, float alpha) {
            if (alpha < 0.4f || scale < 0.5) return;
            g.setColor(withAlpha(new Color(0, 0, 0, 55), alpha));
            g.fillRoundRect((int)n.x + 3, (int)n.y + 3, NW, NH, ARC, ARC);
        }

        private void drawNode(Graphics2D g, DNode n,
                              boolean selected, boolean hovered, float alpha) {
            Color[] cols = palette(n.type);
            Color bg     = withAlpha(cols[0], alpha);
            Color border = withAlpha(selected ? C_SEL_RING : (hovered ? cols[1].brighter() : cols[1]), alpha);

            // Corps
            g.setColor(bg);
            g.fillRoundRect((int)n.x, (int)n.y, NW, NH, ARC, ARC);

            // Dégradé léger en haut
            if (alpha > 0.5f) {
                GradientPaint gp = new GradientPaint(
                    (float)n.x, (float)n.y,         withAlpha(Color.WHITE, alpha * 0.12f),
                    (float)n.x, (float)(n.y + NH/2), withAlpha(Color.WHITE, 0));
                g.setPaint(gp);
                g.fillRoundRect((int)n.x, (int)n.y, NW, NH / 2, ARC, ARC);
                g.setPaint(null);
            }

            // Bordure
            g.setColor(border);
            g.setStroke(new BasicStroke(selected ? 2.5f : (hovered ? 1.8f : 1.2f)));
            g.drawRoundRect((int)n.x, (int)n.y, NW, NH, ARC, ARC);

            // Bande verticale gauche (indicateur de type)
            if (alpha > 0.3f) {
                g.setColor(withAlpha(cols[1], Math.min(alpha + 0.2f, 1f)));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine((int)n.x + 3, (int)n.y + 5,
                           (int)n.x + 3, (int)n.y + NH - 5);
            }

            // Label
            if (alpha > 0.15f) {
                int fontSize = (scale < 0.55) ? 9 : 11;
                g.setFont(new Font(Font.SANS_SERIF,
                    selected ? Font.BOLD : Font.PLAIN, fontSize));
                FontMetrics fm = g.getFontMetrics();
                String lbl = truncate(n.label, fm, NW - 16);
                int lx = (int)n.x + 10;
                int ly = (int)n.y + (NH + fm.getAscent() - fm.getDescent()) / 2;
                // Légère ombre texte
                if (alpha > 0.6f) {
                    g.setColor(withAlpha(new Color(0,0,0,80), alpha));
                    g.drawString(lbl, lx + 1, ly + 1);
                }
                g.setColor(withAlpha(cText(), alpha));
                g.drawString(lbl, lx, ly);
            }
        }

        // ── Tooltip ─────────────────────────────────────────────────────────

        private void drawTooltip(Graphics2D g, DNode n) {
            long connCount = model.edges.stream()
                .filter(e -> e.fromId.equals(n.id) || e.toId.equals(n.id))
                .count();
            String text = n.label + "  [" + typeLbl(n.type) + "]  —  " + connCount + " connexion(s)";

            Font      f  = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text) + 18;
            int th = fm.getHeight() + 10;
            int px = (int)(n.x + NW / 2.0 - tw / 2.0);
            int py = (int)(n.y - th - 8);

            // Fond
            g.setColor(cTooltipBg());
            g.fillRoundRect(px, py, tw, th, 6, 6);
            g.setColor(cTooltipBorder());
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(px, py, tw, th, 6, 6);

            // Texte
            g.setColor(cText());
            g.drawString(text, px + 9, py + fm.getAscent() + 5);
        }

        // ── Helpers ─────────────────────────────────────────────────────────

        private Color withAlpha(Color c, float a) {
            int alpha = Math.max(0, Math.min(255, (int)(c.getAlpha() * a)));
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }

        private Color[] palette(NodeType t) {
            return switch (t) {
                case DIMENSION     -> new Color[]{ C_DIM,  C_DIM_B  };
                case HIERARCHY     -> new Color[]{ C_HIER, C_HIER_B };
                case FILTER        -> new Color[]{ C_FILT, C_FILT_B };
                case IMPORT_EXPORT -> new Color[]{ C_IE,   C_IE_B   };
            };
        }

        private String typeLbl(NodeType t) {
            return switch (t) {
                case DIMENSION     -> "Dimension";
                case HIERARCHY     -> "Hiérarchie";
                case FILTER        -> "Filtre";
                case IMPORT_EXPORT -> "Import/Export";
            };
        }

        private String truncate(String s, FontMetrics fm, int maxW) {
            if (fm.stringWidth(s) <= maxW) return s;
            while (s.length() > 1 && fm.stringWidth(s + "…") > maxW)
                s = s.substring(0, s.length() - 1);
            return s + "…";
        }

        private Set<String> highlighted() {
            if (selectedId == null) {
                if (hoveredId == null) return Collections.emptySet();
                return buildNeighbours(hoveredId);
            }
            return buildNeighbours(selectedId);
        }

        private Set<String> buildNeighbours(String id) {
            Set<String> ids = new HashSet<>();
            ids.add(id);
            for (DEdge e : model.edges) {
                if (e.fromId.equals(id)) ids.add(e.toId);
                if (e.toId.equals(id))   ids.add(e.fromId);
            }
            return ids;
        }

        private boolean matchesFilter(DNode n) {
            return filterText.isEmpty()
                || n.label.toLowerCase(Locale.ROOT).contains(filterText);
        }

        // ── Coordonnées ─────────────────────────────────────────────────────

        private Point2D worldPt(Point p) {
            return new Point2D.Double((p.x - tx) / scale, (p.y - ty) / scale);
        }

        private String hitTest(Point2D w) {
            for (DNode n : model.nodes.values())
                if (w.getX() >= n.x && w.getX() <= n.x + NW
                 && w.getY() >= n.y && w.getY() <= n.y + NH)
                    return n.id;
            return null;
        }

        // ── Interactions ────────────────────────────────────────────────────

        private void onPress(MouseEvent e) {
            dragPt = e.getPoint(); dragTX = tx; dragTY = ty;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        private void onDrag(MouseEvent e) {
            if (dragPt == null) return;
            tx = dragTX + (e.getX() - dragPt.x);
            ty = dragTY + (e.getY() - dragPt.y);
            repaint();
        }

        private void onClick(MouseEvent e) {
            String hit = hitTest(worldPt(e.getPoint()));
            selectedId = Objects.equals(hit, selectedId) ? null : hit;
            if (infoLabel != null) {
                if (selectedId != null) {
                    DNode n = model.nodes.get(selectedId);
                    long  c = model.edges.stream()
                        .filter(ed -> ed.fromId.equals(selectedId) || ed.toId.equals(selectedId))
                        .count();
                    infoLabel.setText("  " + n.label + "  —  " + typeLbl(n.type)
                        + "  ·  " + c + " connexion(s)");
                } else {
                    infoLabel.setText(" ");
                }
            }
            repaint();
        }

        private void onMove(MouseEvent e) {
            String hit = hitTest(worldPt(e.getPoint()));
            if (!Objects.equals(hit, hoveredId)) {
                hoveredId = hit;
                setCursor(hit != null
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
                repaint();
            }
        }

        private void onWheel(MouseWheelEvent e) {
            double factor   = (e.getWheelRotation() < 0) ? 1.12 : 0.90;
            double newScale = Math.max(0.12, Math.min(5.0, scale * factor));
            // Zoom centré sur le curseur
            double mx = e.getX(), my = e.getY();
            tx = mx - (mx - tx) * (newScale / scale);
            ty = my - (my - ty) * (newScale / scale);
            scale = newScale;
            repaint();
        }
    }
}
