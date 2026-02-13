package com.stilog.analysevpi.model;

import java.io.File;

import com.stilog.vpimodel.objects.PositionFile;
import com.stilog.vpimodel.objects.VPIDatas;

public class ComparisonModel {

	private VPIDatas gauche = new VPIDatas();
	private VPIDatas droite = new VPIDatas();
	
	public void processFile(File file, PositionFile position) {
		getData(position).computeDatas(file, position);
		if(position == PositionFile.RIGHT) {
			gauche.reset();
		}
	}
	
	/*
	 * GETTER & SETTER
	 */
	public VPIDatas getData(PositionFile position) {
		switch(position) {
		case LEFT:
			return gauche;
		case RIGHT:
			return droite;
		default:
			return null;
		}
	}
	
	public void reverse() {
		VPIDatas prevLeft = gauche;
		
		this.gauche = this.droite;
		this.droite = prevLeft;
		
		if(this.droite.isParsed())
			this.droite.reset();
	}
	
	public void setGauche(VPIDatas gauche) {
		this.gauche = gauche;
	}

	public void setDroite(VPIDatas droite) {
		this.droite = droite;
	}
	
	
}
