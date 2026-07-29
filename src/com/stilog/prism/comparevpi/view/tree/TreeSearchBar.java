package com.stilog.prism.comparevpi.view.tree;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

import com.stilog.prism.view.ThemeManager;

/**
 * Barre de recherche générique pour un {@link JTree} : recherche par
 * sous-chaîne insensible à la casse sur le {@code toString()} de chaque
 * nœud (Entity/Parameters/Attribute/FileDatas ont tous un toString()
 * lisible incluant leur nom), navigation suivant/précédent, expansion +
 * sélection + scroll automatique du nœud trouvé.
 */
public class TreeSearchBar extends JPanel {

    private static final long serialVersionUID = 1L;
    private final ThemeManager theme = ThemeManager.getInstance();

    private final JTree tree;
    private final JTextField searchField;
    private final JLabel countLabel;

    private final List<TreePath> matches = new ArrayList<>();
    private int currentIndex = -1;

    public TreeSearchBar(JTree tree) {
        this.tree = tree;
        setLayout(new BorderLayout(6, 0));
        setOpaque(false);

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher dans l'arbre…");
        searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        countLabel = new JLabel("");
        countLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        JButton prevBtn = iconButton(MaterialDesign.MDI_CHEVRON_LEFT, "Occurrence précédente");
        JButton nextBtn = iconButton(MaterialDesign.MDI_CHEVRON_RIGHT, "Occurrence suivante");
        JButton clearBtn = iconButton(MaterialDesign.MDI_CLOSE_CIRCLE, "Effacer la recherche");

        prevBtn.addActionListener(e -> selectRelative(-1));
        nextBtn.addActionListener(e -> selectRelative(1));
        clearBtn.addActionListener(e -> searchField.setText(""));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { performSearch(); }
            @Override public void removeUpdate(DocumentEvent e)  { performSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { performSearch(); }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    selectRelative(e.isShiftDown() ? -1 : 1);
            }
        });

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        navPanel.setOpaque(false);
        navPanel.add(countLabel);
        navPanel.add(prevBtn);
        navPanel.add(nextBtn);
        navPanel.add(clearBtn);

        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        JLabel searchIcon = new JLabel(FontIcon.of(MaterialDesign.MDI_MAGNIFY, 14, theme.textDim()));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        iconWrap.add(searchIcon, BorderLayout.WEST);
        iconWrap.add(searchField, BorderLayout.CENTER);

        add(iconWrap, BorderLayout.CENTER);
        add(navPanel, BorderLayout.EAST);

        applyThemeColors();
        theme.addChangeListener(this::applyThemeColors);
    }

    private void applyThemeColors() {
        countLabel.setForeground(theme.textDim());
    }

    private JButton iconButton(MaterialDesign icon, String tooltip) {
        JButton btn = new JButton(FontIcon.of(icon, 14, theme.textDim()));
        btn.setToolTipText(tooltip);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** À rappeler après tout rechargement du modèle de l'arbre (le contenu affiché a changé). */
    public void refresh() {
        performSearch();
    }

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        matches.clear();
        currentIndex = -1;

        if (!query.isEmpty() && tree.getModel() != null && tree.getModel().getRoot() != null) {
            TreeNode root = (TreeNode) tree.getModel().getRoot();
            collect(new TreePath(root), query, matches);
        }

        if (!matches.isEmpty())
            selectMatch(0);
        else
            updateCountLabel();
    }

    private void collect(TreePath path, String queryLower, List<TreePath> out) {
        TreeNode node = (TreeNode) path.getLastPathComponent();
        String text = node.toString();
        if (text != null && text.toLowerCase().contains(queryLower))
            out.add(path);
        for (int i = 0; i < node.getChildCount(); i++)
            collect(path.pathByAddingChild(node.getChildAt(i)), queryLower, out);
    }

    private void selectRelative(int delta) {
        if (matches.isEmpty())
            return;
        selectMatch((currentIndex + delta + matches.size()) % matches.size());
    }

    private void selectMatch(int index) {
        currentIndex = index;
        TreePath path = matches.get(index);
        for (TreePath p = path.getParentPath(); p != null; p = p.getParentPath())
            tree.expandPath(p);
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        updateCountLabel();
    }

    private void updateCountLabel() {
        countLabel.setText(matches.isEmpty()
            ? (searchField.getText().isBlank() ? "" : "0/0")
            : (currentIndex + 1) + "/" + matches.size());
    }
}
