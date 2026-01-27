package com.stilog.analysevpi.view.loading;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Icône de chargement animée avec trois points
 */
public class LoadingIcon implements Icon {
    
    private static final int SIZE = 20;
    private static final int DOT_SIZE = 4;
    private static final int DOT_SPACING = 7;
    private static final Color DOT_COLOR = Color.GRAY;
    
    private int currentDot = 0;
    private Timer animationTimer;
    
    public LoadingIcon() {
        // Timer pour l'animation (change toutes les 300ms)
        animationTimer = new Timer(300, e -> {
            currentDot = (currentDot + 1) % 3;
        });
    }
    
    /**
     * Démarre l'animation
     */
    public void start(JComponent component) {
        if (!animationTimer.isRunning()) {
            animationTimer.start();
            // Redessiner le composant à chaque tick
            animationTimer.addActionListener(e -> component.repaint());
        }
    }
    
    /**
     * Arrête l'animation
     */
    public void stop() {
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
    
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Calculer la position de départ pour centrer les trois points
        int totalWidth = (DOT_SIZE * 3) + (DOT_SPACING * 2);
        int startX = x + (SIZE - totalWidth) / 2;
        int centerY = y + SIZE / 2;
        
        // Dessiner les trois points
        for (int i = 0; i < 3; i++) {
            int dotX = startX + (i * (DOT_SIZE + DOT_SPACING));
            int dotY = centerY - DOT_SIZE / 2;
            
            // Le point actuel est plus grand et plus foncé
            if (i == currentDot) {
                g2d.setColor(DOT_COLOR.darker());
                Ellipse2D dot = new Ellipse2D.Double(dotX - 1, dotY - 1, DOT_SIZE + 2, DOT_SIZE + 2);
                g2d.fill(dot);
            } else {
                g2d.setColor(DOT_COLOR.brighter());
                Ellipse2D dot = new Ellipse2D.Double(dotX, dotY, DOT_SIZE, DOT_SIZE);
                g2d.fill(dot);
            }
        }
        
        g2d.dispose();
    }
    
    @Override
    public int getIconWidth() {
        return SIZE;
    }
    
    @Override
    public int getIconHeight() {
        return SIZE;
    }
}