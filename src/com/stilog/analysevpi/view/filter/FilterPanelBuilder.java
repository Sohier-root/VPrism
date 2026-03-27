package com.stilog.analysevpi.view.filter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.stilog.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.vpimodel.objects.filter.FilterLeafNode;
import com.stilog.vpimodel.objects.filter.FilterNode;

/**
 * Construit le panneau de rendu visuel d'un filtre VP.
 * Utilisé à la fois par FilterDialog (filtre seul)
 * et FilterCompareDialog (deux filtres côte à côte).
 */
public class FilterPanelBuilder {

    // Couleurs des groupes par profondeur
    private static final Color[] GROUP_ACCENT = {
        new Color(52,  120, 210),
        new Color(130, 60,  200),
        new Color(20,  160, 120),
        new Color(200, 110, 20),
        new Color(180, 40,  40),
    };

    static final Color BG_PANEL   = new Color(246, 247, 249);
    static final Color BG_GROUP   = new Color(255, 255, 255);
    static final Color ATTR_FG    = new Color(25,  30,  45);
    static final Color DYNAMIC_FG = new Color(110, 120, 135);

    private static final Color[] OP_EQUALS  = { new Color(232, 245, 232), new Color(28, 110, 28),  new Color(155, 210, 155) };
    private static final Color[] OP_COMPARE = { new Color(230, 240, 255), new Color(25,  70, 170), new Color(155, 190, 240) };
    private static final Color[] OP_IN      = { new Color(243, 232, 255), new Color(90,  28, 160), new Color(195, 155, 240) };
    private static final Color[] OP_TEXT    = { new Color(255, 250, 228), new Color(140, 88,  0),  new Color(230, 190, 95)  };
    private static final Color[] OP_NOT     = { new Color(255, 233, 233), new Color(170, 28,  28), new Color(240, 165, 165) };

    static final Color VALUE_BG     = new Color(244, 245, 248);
    static final Color VALUE_FG     = new Color(35,  42,  55);
    static final Color VALUE_BORDER = new Color(208, 214, 222);

    /**
     * Construit un JScrollPane contenant le rendu complet d'un filtre.
     * Retourne un panneau avec titre + conditions scrollables.
     */
    public static JPanel buildFilterPanel(String filterName, FilterGroupNode root) {
        JPanel outer = new JPanel(new java.awt.BorderLayout(0, 0));
        outer.setBackground(BG_PANEL);
        outer.setBorder(new EmptyBorder(12, 12, 8, 12));

        // Titre
        JLabel title = new JLabel(filterName != null ? filterName : "(aucun filtre)");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setForeground(ATTR_FG);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        outer.add(title, java.awt.BorderLayout.NORTH);

        // Contenu
        JPanel content;
        if (root == null) {
            content = new JPanel();
            content.setBackground(BG_PANEL);
            JLabel empty = new JLabel("Filtre absent dans ce VPI.");
            empty.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            empty.setForeground(DYNAMIC_FG);
            content.add(empty);
        } else {
            JPanel groupPanel = buildGroupPanel(root, 0);
            groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(BG_PANEL);
            content.add(groupPanel);
            content.add(Box.createVerticalGlue());
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_PANEL);
        scroll.getViewport().setBackground(BG_PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        outer.add(scroll, java.awt.BorderLayout.CENTER);

        return outer;
    }

    // -----------------------------------------------------------------------
    // Rendu récursif (package-private pour réutilisation dans FilterDialog)
    // -----------------------------------------------------------------------

    static JPanel buildGroupPanel(FilterGroupNode group, int depth) {
        Color accent = GROUP_ACCENT[Math.min(depth, GROUP_ACCENT.length - 1)];

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (group.isEmpty()) {
            JLabel empty = new JLabel("Aucune condition définie.");
            empty.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            empty.setForeground(DYNAMIC_FG);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(empty);
            return container;
        }

        // En-tête badge ET/OU
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean isOr = "OR".equals(group.getOperator());
        String badgeLabel = isOr ? "OU" : "ET";
        String desc = isOr ? "L'une des conditions est vraie" : "Toutes les conditions sont vraies";

        Color badgeBg     = blend(accent, Color.WHITE, 0.88f);
        Color badgeBorder = blend(accent, Color.WHITE, 0.55f);
        header.add(new RoundedBadge(badgeLabel, badgeBg, accent, badgeBorder,
                new Font(Font.SANS_SERIF, Font.BOLD, 12)));

        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        descLabel.setForeground(DYNAMIC_FG);
        header.add(descLabel);

        container.add(header);
        container.add(Box.createVerticalStrut(6));

        // Bloc contenu
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBackground(BG_GROUP);
        block.setBorder(BorderFactory.createCompoundBorder(
            new LeftColorBorder(accent, 3),
            new EmptyBorder(8, 12, 8, 12)
        ));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        for (int i = 0; i < group.getChildren().size(); i++) {
            FilterNode child = group.getChildren().get(i);
            if (child.isGroup()) {
                JPanel sub = buildGroupPanel((FilterGroupNode) child, depth + 1);
                sub.setAlignmentX(Component.LEFT_ALIGNMENT);
                block.add(sub);
            } else if (child.isLeaf()) {
                JPanel leaf = buildLeafCard((FilterLeafNode) child);
                leaf.setAlignmentX(Component.LEFT_ALIGNMENT);
                block.add(leaf);
            }
            if (i < group.getChildren().size() - 1) {
                block.add(Box.createVerticalStrut(4));
            }
        }

        container.add(block);
        return container;
    }

    static JPanel buildLeafCard(FilterLeafNode leaf) {
        JPanel card = new RoundedPanel(6, new Color(250, 251, 253), new Color(218, 223, 230));
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 5));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel attrLabel = new JLabel(leaf.getAttributeTitle().isEmpty() ? "(attribut)" : leaf.getAttributeTitle());
        attrLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        attrLabel.setForeground(ATTR_FG);
        card.add(attrLabel);

        Color[] opColors = resolveOperatorColors(leaf.getOperator());
        card.add(new RoundedBadge(translateOperator(leaf.getOperator()),
                opColors[0], opColors[1], opColors[2],
                new Font(Font.SANS_SERIF, Font.BOLD, 11)));

        String val = leaf.getValueDisplay();
        if (val.isEmpty() && leaf.isDynamic()) val = "(valeur dynamique)";

        if (!val.isEmpty()) {
            if (val.startsWith("$") || val.equals("(valeur dynamique)") || val.equals("(null)")) {
                JLabel dynLabel = new JLabel(val);
                dynLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
                dynLabel.setForeground(DYNAMIC_FG);
                card.add(dynLabel);
            } else {
                card.add(new RoundedBadge(val, VALUE_BG, VALUE_FG, VALUE_BORDER,
                        new Font(Font.SANS_SERIF, Font.PLAIN, 12)));
            }
        }
        return card;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    static Color blend(Color a, Color b, float ratioB) {
        float ra = 1f - ratioB;
        return new Color(
            Math.min(255, Math.round(a.getRed()   * ra + b.getRed()   * ratioB)),
            Math.min(255, Math.round(a.getGreen() * ra + b.getGreen() * ratioB)),
            Math.min(255, Math.round(a.getBlue()  * ra + b.getBlue()  * ratioB))
        );
    }

    static Color[] resolveOperatorColors(String op) {
        switch (op) {
            case "EQUALS":        return OP_EQUALS;
            case "NOTEQUALS":     return OP_NOT;
            case "IN":            return OP_IN;
            case "INFILTER":      return OP_IN;
            case "NOTINFILTER":   return OP_NOT;
            case "NOTIN":         return OP_NOT;
            case "STARTWITHS":    return OP_TEXT;
            case "NOTSTARTWITHS": return OP_NOT;
            case "CONTENTS":      return OP_TEXT;
            case "NOTCONTENTS":   return OP_NOT;
            case "LIKE":          return OP_EQUALS;
            case "GREATEREQUALS": return OP_COMPARE;
            case "LESSEREQUALS":  return OP_COMPARE;
            case "GREATER":       return OP_COMPARE;
            case "LESSER":        return OP_COMPARE;
            case "ATLEASTONE":    return OP_IN;
            case "NONE":          return OP_NOT;
            case "ISA":           return OP_COMPARE;
            default:              return OP_EQUALS;
        }
    }

    static String translateOperator(String op) {
        switch (op) {
            case "EQUALS":        return "=";
            case "NOTEQUALS":     return "≠";
            case "IN":            return "est dans";
            case "INFILTER":      return "est dans le filtre";
            case "NOTINFILTER":   return "n'est pas dans le filtre";
            case "NOTIN":         return "n'est pas dans";
            case "STARTWITHS":    return "commence par";
            case "NOTSTARTWITHS": return "ne commence pas par";
            case "CONTENTS":      return "contient";
            case "NOTCONTENTS":   return "ne contient pas";
            case "LIKE":          return "égal à";
            case "GREATEREQUALS": return "≥";
            case "LESSEREQUALS":  return "≤";
            case "GREATER":       return ">";
            case "LESSER":        return "<";
            case "ATLEASTONE":    return "au moins un";
            case "NONE":          return "aucun";
            case "ISA":           return "est de type";
            default:              return op;
        }
    }

    // -----------------------------------------------------------------------
    // Composants graphiques internes
    // -----------------------------------------------------------------------

    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg, border;
        RoundedPanel(int radius, Color bg, Color border) {
            this.radius = radius; this.bg = bg; this.border = border;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radius*2, radius*2);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius*2, radius*2);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedBadge extends JPanel {
        private final String text;
        private final Color bg, fg, border;
        private final Font font;
        RoundedBadge(String text, Color bg, Color fg, Color border, Font font) {
            this.text = text; this.bg = bg; this.fg = fg;
            this.border = border; this.font = font;
            setOpaque(false);
            java.awt.FontMetrics fm = getFontMetrics(font);
            setPreferredSize(new Dimension(fm.stringWidth(text) + 20, fm.getHeight() + 8));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
            g2.setFont(font);
            g2.setColor(fg);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    static class LeftColorBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int thickness;
        LeftColorBorder(Color color, int thickness) {
            this.color = color; this.thickness = thickness;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(color);
            g2.fillRoundRect(x, y + 4, thickness, h - 8, thickness, thickness);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(4, thickness + 6, 4, 0);
        }
        @Override public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(4, thickness + 6, 4, 0); return insets;
        }
    }
}
