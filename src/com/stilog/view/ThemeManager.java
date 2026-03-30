package com.stilog.view;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Gestionnaire de thème (clair / sombre) — singleton.
 * <p>
 * Les composants UI s'enregistrent via {@link #addChangeListener(Runnable)}
 * pour être notifiés et se repeindre quand le thème change.
 * <p>
 * Usage depuis n'importe quel composant :
 * <pre>
 *   Color bg = ThemeManager.getInstance().bg();
 * </pre>
 */
public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static ThemeManager instance;
    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private static final String PREF_KEY = "theme";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private Theme current;
    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {
        // Restaure le thème sauvegardé, LIGHT par défaut
        String saved = prefs.get(PREF_KEY, Theme.LIGHT.name());
        current = Theme.valueOf(saved);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Theme getTheme() { return current; }
    public boolean isDark()  { return current == Theme.DARK; }

    /**
     * Bascule le thème et notifie tous les listeners enregistrés.
     * Appeler depuis l'EDT.
     */
    public void toggle(java.awt.Window rootWindow) {
        current = isDark() ? Theme.LIGHT : Theme.DARK;
        prefs.put(PREF_KEY, current.name());  // persistance
        applyLaf();
        if (rootWindow != null) SwingUtilities.updateComponentTreeUI(rootWindow);
        notifyListeners();
    }

    public void addChangeListener(Runnable r)    { listeners.add(r); }
    public void removeChangeListener(Runnable r) { listeners.remove(r); }

    // ── Palette accessors ─────────────────────────────────────────────────────

    /** Fond global de la page */
    public Color bg()          { return isDark() ? new Color(22, 26, 34)    : new Color(242, 244, 248); }
    /** Fond des cartes / panneaux */
    public Color cardBg()      { return isDark() ? new Color(30, 35, 46)    : Color.WHITE; }
    /** Bordure des cartes */
    public Color cardBorder()  { return isDark() ? new Color(48, 55, 72)    : new Color(210, 215, 225); }
    /** Couleur d'accentuation principale (bleu) */
    public Color accent()      { return isDark() ? new Color(52, 120, 215)  : new Color(37, 99, 195); }
    /** Accentuation au survol */
    public Color accentHover() { return isDark() ? new Color(72, 145, 245)  : new Color(59, 130, 246); }
    /** Texte principal */
    public Color text()        { return isDark() ? new Color(210, 218, 235) : new Color(30, 35, 50); }
    /** Texte secondaire / atténué */
    public Color textDim()     { return isDark() ? new Color(130, 145, 170) : new Color(100, 110, 130); }
    /** Fond des champs de texte */
    public Color fieldBg()     { return isDark() ? new Color(40, 46, 58)    : new Color(248, 249, 252); }
    /** Bordure des champs */
    public Color fieldBorder() { return isDark() ? new Color(60, 68, 86)    : new Color(195, 202, 215); }
    /** Couleur de séparation */
    public Color separator()   { return isDark() ? new Color(45, 52, 68)    : new Color(220, 224, 232); }
    /** Fond du header */
    public Color headerBg()    { return isDark() ? new Color(28, 32, 42)    : new Color(255, 255, 255); }
    /** Bordure basse du header */
    public Color headerBorder(){ return isDark() ? new Color(45, 52, 68)    : new Color(210, 215, 225); }
    /** Texte du titre */
    public Color titleText()   { return isDark() ? new Color(235, 240, 255) : new Color(15, 20, 35); }
    /** Badge zone Référence */
    public Color badgeRef()    { return new Color(52, 120, 215); }
    /** Badge zone Testé */
    public Color badgeTest()   { return new Color(34, 160, 100); }
    /** Texte inactif dans la nav */
    public Color navInactive() { return isDark() ? new Color(130, 145, 170) : new Color(60, 70, 90); }

    // ── Private ───────────────────────────────────────────────────────────────

    /** Appelé une seule fois au démarrage depuis Main, avant la création de la fenêtre. */
    public void applyLafOnStartup() {
        applyLaf();
    }

    private void applyLaf() {
        try {
            if (isDark()) FlatDarkLaf.setup();
            else           FlatLightLaf.setup();

            UIManager.put("TabbedPane.tabHeight", 36);
            UIManager.put("ScrollBar.width", 8);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("TabbedPane.selectedBackground",
                isDark() ? new Color(52, 120, 215, 60) : new Color(37, 99, 195, 40));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyListeners() {
        for (Runnable r : listeners) r.run();
    }
}
