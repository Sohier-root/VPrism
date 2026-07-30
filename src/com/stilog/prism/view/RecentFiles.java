package com.stilog.prism.view;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Liste des derniers fichiers VPI/VPS ouverts, persistée via {@link Preferences}
 * et partagée entre les écrans qui ouvrent un fichier (VPI unique, comparaison
 * gauche/droite).
 */
public class RecentFiles {

    private static final int MAX_ENTRIES = 8;
    private static final String PREF_KEY = "recentFiles";
    private static final String SEPARATOR = "\n";

    private static final Preferences prefs = Preferences.userNodeForPackage(RecentFiles.class);

    private RecentFiles() {
    }

    public static List<File> get() {
        List<File> result = new ArrayList<>();
        String raw = prefs.get(PREF_KEY, "");
        if (raw.isBlank())
            return result;
        for (String path : raw.split(SEPARATOR)) {
            if (!path.isBlank())
                result.add(new File(path));
        }
        return result;
    }

    public static void add(File file) {
        if (file == null)
            return;

        LinkedHashSet<String> paths = new LinkedHashSet<>();
        paths.add(file.getAbsolutePath());
        for (File existing : get()) {
            if (paths.size() >= MAX_ENTRIES)
                break;
            paths.add(existing.getAbsolutePath());
        }

        prefs.put(PREF_KEY, String.join(SEPARATOR, paths));
    }

    /** Construit le menu "fichiers récents" à afficher sous un bouton dédié. */
    public static JPopupMenu buildMenu(Consumer<File> onSelect) {
        JPopupMenu menu = new JPopupMenu();
        List<File> files = get();

        if (files.isEmpty()) {
            JMenuItem empty = new JMenuItem("Aucun fichier récent");
            empty.setEnabled(false);
            menu.add(empty);
            return menu;
        }

        for (File f : files) {
            JMenuItem item = new JMenuItem(f.getName());
            item.setToolTipText(f.getAbsolutePath());
            item.addActionListener(e -> onSelect.accept(f));
            menu.add(item);
        }
        return menu;
    }
}
