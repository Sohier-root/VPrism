package com.stilog.prism.comparevpi.view.object;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.Timer;

public class VButton extends JButton{

	public VButton(String name) {
		super(name);
	}
	
	@Override
	public void setEnabled(boolean enable) {
		boolean isEnable = this.isEnabled();
		super.setEnabled(enable);
		if(!isEnable && enable == true)
			animateButton();
	}
	
	/**
	 * Anime le bouton avec un effet de fondu
	 */
	private void animateButton() {
	    Color originalBackground = this.getBackground();
	    Color highlightColor = new Color(128, 141, 255);
	    
	    // Animation progressive
	    Timer timer = new Timer(20, null);
	    final int[] step = {0};
	    final int maxSteps = 17;
	    
	    timer.addActionListener(e -> {
	        step[0]++;
	        
	        if (step[0] <= maxSteps) {
	            // Fondu vers la couleur highlight
	            float ratio = step[0] / (float) maxSteps;
	            Color interpolated = interpolateColor(originalBackground, highlightColor, ratio);
	            this.setBackground(interpolated);
	            this.setOpaque(true);
	        } else if (step[0] <= maxSteps * 2) {
	            // Fondu retour vers la couleur d'origine
	            float ratio = (step[0] - maxSteps) / (float) maxSteps;
	            Color interpolated = interpolateColor(highlightColor, originalBackground, ratio);
	            this.setBackground(interpolated);
	        } else {
	            // Fin de l'animation
	            this.setBackground(originalBackground);
	            this.setOpaque(false);
	            ((Timer) e.getSource()).stop();
	        }
	    });
	    
	    timer.start();
	}

	/**
	 * Interpolation entre deux couleurs
	 */
	private Color interpolateColor(Color start, Color end, float ratio) {
	    int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
	    int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
	    int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
	    return new Color(red, green, blue);
	}
}
