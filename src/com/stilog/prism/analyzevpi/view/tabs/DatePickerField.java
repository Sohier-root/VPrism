package com.stilog.prism.analyzevpi.view.tabs;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.stilog.prism.view.ThemeManager;

/**
 * Champ de saisie de date avec calendrier popup natif Swing.
 *
 * <p>Affiche un bouton "📅" à droite du champ texte. Au clic, un popup
 * calendrier mensuel apparaît sous le champ. La date sélectionnée est
 * écrite dans le champ au format {@code yyyy-MM-dd}.
 *
 * <p>S'intègre au {@link ThemeManager} pour respecter le thème clair/sombre.
 * Des listeners peuvent être ajoutés via {@link #addDateChangeListener}.
 */
public class DatePickerField extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ThemeManager theme = ThemeManager.getInstance();

    private final JTextField   textField;
    private final JButton      calBtn;
    private       JWindow      popup;
    private       LocalDate    selectedDate;

    // Mois affiché dans le popup
    private YearMonth displayedMonth;

    private final List<Runnable> changeListeners = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    public DatePickerField() {
        super(new BorderLayout(0, 0));
        setOpaque(false);

        textField = new JTextField(10);
        styleField(textField);
        textField.setEditable(false);
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        textField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { togglePopup(); }
        });

        calBtn = new JButton("📅");
        calBtn.setFont(calBtn.getFont().deriveFont(13f));
        calBtn.setFocusPainted(false);
        calBtn.setBorderPainted(false);
        calBtn.setContentAreaFilled(false);
        calBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calBtn.setForeground(theme.textDim());
        calBtn.addActionListener(e -> togglePopup());

        add(textField, BorderLayout.CENTER);
        add(calBtn,    BorderLayout.EAST);

        displayedMonth = YearMonth.now();

        theme.addChangeListener(this::restyle);
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /** Retourne la date sélectionnée, ou {@code null} si aucune. */
    public LocalDate getDate() { return selectedDate; }

    /** Définit la date programmatiquement. */
    public void setDate(LocalDate date) {
        selectedDate = date;
        textField.setText(date != null ? date.format(FMT) : "");
    }

    /** Vide la sélection. */
    public void clear() { setDate(null); }

    /** Ajoute un listener appelé quand la date change. */
    public void addDateChangeListener(Runnable r) { changeListeners.add(r); }

    /** Vrai si une date est sélectionnée. */
    public boolean hasDate() { return selectedDate != null; }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        calBtn.setEnabled(enabled);
        if (!enabled && popup != null) popup.setVisible(false);
    }

    // ── Popup calendrier ──────────────────────────────────────────────────────

    private void togglePopup() {
        if (!isEnabled()) return;
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        showPopup();
    }

    private void showPopup() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        popup = new JWindow(parent);
        popup.setFocusableWindowState(false);
        popup.add(buildCalendarPanel());
        popup.pack();

        // Positionner sous le champ
        Point loc = getLocationOnScreen();
        int px = loc.x;
        int py = loc.y + getHeight() + 2;

        // Rester dans l'écran
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();
        if (px + popup.getWidth() > screen.x + screen.width)
            px = screen.x + screen.width - popup.getWidth();
        if (py + popup.getHeight() > screen.y + screen.height)
            py = loc.y - popup.getHeight() - 2;

        popup.setLocation(px, py);
        popup.setVisible(true);

        // Fermer si clic en dehors
        Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener,
            AWTEvent.MOUSE_EVENT_MASK);
    }

    private final AWTEventListener outsideClickListener = event -> {
        if (event instanceof MouseEvent me && me.getID() == MouseEvent.MOUSE_PRESSED) {
            if (popup != null && popup.isVisible()) {
                Point p = me.getLocationOnScreen();
                Rectangle r = popup.getBounds();
                if (!r.contains(p)) {
                    closePopup();
                }
            }
        }
    };

    private void closePopup() {
        if (popup != null) popup.setVisible(false);
        Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
    }

    // ── Construction du panneau calendrier ────────────────────────────────────

    private JPanel buildCalendarPanel() {
        JPanel cal = new JPanel(new BorderLayout(0, 4));
        cal.setBackground(theme.cardBg());
        cal.setBorder(new LineBorder(theme.cardBorder(), 1));
        cal.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.cardBorder(), 1),
            new EmptyBorder(6, 8, 8, 8)));

        cal.add(buildNavBar(),    BorderLayout.NORTH);
        cal.add(buildDaysGrid(),  BorderLayout.CENTER);
        cal.add(buildFooter(),    BorderLayout.SOUTH);

        return cal;
    }

    /** Barre de navigation mois/année. */
    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new BorderLayout(0, 0));
        nav.setOpaque(false);

        JButton prev = navBtn("‹");
        JButton next = navBtn("›");

        JLabel monthLabel = new JLabel(formatMonth(displayedMonth), SwingConstants.CENTER);
        monthLabel.setForeground(theme.text());
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 13f));

        prev.addActionListener(e -> {
            displayedMonth = displayedMonth.minusMonths(1);
            refreshPopup();
        });
        next.addActionListener(e -> {
            displayedMonth = displayedMonth.plusMonths(1);
            refreshPopup();
        });

        nav.add(prev,       BorderLayout.WEST);
        nav.add(monthLabel, BorderLayout.CENTER);
        nav.add(next,       BorderLayout.EAST);
        return nav;
    }

    /** Grille des jours. */
    private JPanel buildDaysGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setOpaque(false);

        // En-têtes jours
        String[] dayNames = {"Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"};
        for (String d : dayNames) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setForeground(theme.textDim());
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
            grid.add(lbl);
        }

        LocalDate first = displayedMonth.atDay(1);
        // Décalage : lundi=1, …, dimanche=7
        int startOffset = first.getDayOfWeek().getValue() - 1;

        // Cases vides avant le 1er
        for (int i = 0; i < startOffset; i++) grid.add(emptyCell());

        LocalDate today = LocalDate.now();
        int daysInMonth = displayedMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = displayedMonth.atDay(day);
            JButton btn = dayBtn(day, date, today);
            grid.add(btn);
        }

        return grid;
    }

    private JButton dayBtn(int day, LocalDate date, LocalDate today) {
        JButton btn = new JButton(String.valueOf(day));
        btn.setPreferredSize(new Dimension(28, 24));
        btn.setFont(btn.getFont().deriveFont(12f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean isSelected = date.equals(selectedDate);
        boolean isToday    = date.equals(today);

        if (isSelected) {
            btn.setBackground(theme.accent());
            btn.setForeground(Color.WHITE);
            btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        } else if (isToday) {
            btn.setBackground(theme.cardBg());
            btn.setForeground(theme.accent());
            btn.setFont(btn.getFont().deriveFont(Font.BOLD));
            btn.setBorder(new LineBorder(theme.accent(), 1));
            btn.setBorderPainted(true);
        } else {
            btn.setBackground(theme.cardBg());
            btn.setForeground(theme.text());
        }

        btn.addActionListener(e -> {
            selectedDate = date;
            textField.setText(date.format(FMT));
            closePopup();
            changeListeners.forEach(Runnable::run);
        });

        // Hover
        btn.addMouseListener(new MouseAdapter() {
            final Color orig = btn.getBackground();
            @Override public void mouseEntered(MouseEvent e) {
                if (!date.equals(selectedDate))
                    btn.setBackground(theme.fieldBg());
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(orig);
            }
        });

        return btn;
    }

    /** Pied : bouton "Aujourd'hui" + "Effacer". */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        footer.setOpaque(false);

        JButton todayBtn = footerBtn("Aujourd'hui");
        todayBtn.addActionListener(e -> {
            selectedDate = LocalDate.now();
            textField.setText(selectedDate.format(FMT));
            closePopup();
            changeListeners.forEach(Runnable::run);
        });

        JButton clearBtn = footerBtn("Effacer");
        clearBtn.addActionListener(e -> {
            selectedDate = null;
            textField.setText("");
            closePopup();
            changeListeners.forEach(Runnable::run);
        });

        footer.add(todayBtn);
        footer.add(clearBtn);
        return footer;
    }

    /** Recrée le popup pour refléter le nouveau mois. */
    private void refreshPopup() {
        if (popup == null) return;
        Point loc = popup.getLocation();
        popup.getContentPane().removeAll();
        popup.add(buildCalendarPanel());
        popup.pack();
        popup.setLocation(loc);
        popup.revalidate();
        popup.repaint();
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private JButton navBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(theme.textDim());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(28, 22));
        return b;
    }

    private JButton footerBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(11f));
        b.setFocusPainted(false);
        b.setForeground(theme.textDim());
        b.setBackground(theme.fieldBg());
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.cardBorder(), 1),
            new EmptyBorder(2, 8, 2, 8)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel emptyCell() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(28, 24));
        return p;
    }

    private String formatMonth(YearMonth ym) {
        String[] months = {"Janvier","Février","Mars","Avril","Mai","Juin",
                           "Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
        return months[ym.getMonthValue() - 1] + " " + ym.getYear();
    }

    private void styleField(JTextField f) {
        f.setBackground(theme.fieldBg());
        f.setForeground(theme.text());
        f.setCaretColor(theme.text());
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(theme.fieldBorder(), 1),
            new EmptyBorder(3, 8, 3, 8)));
    }

    private void restyle() {
        styleField(textField);
        calBtn.setForeground(theme.textDim());
        repaint();
    }
}
