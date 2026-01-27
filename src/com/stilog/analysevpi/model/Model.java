package com.stilog.analysevpi.model;

import java.io.File;

import com.stilog.analysevpi.model.objects.PositionFile;
import com.stilog.analysevpi.model.objects.VPIDatas;

public class Model {

	private VPIDatas gauche = new VPIDatas();
	private VPIDatas droite = new VPIDatas();
	
	public void processFile(File file, PositionFile position) {
		getData(position).computeDatas(file, position);
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
	public void setGauche(VPIDatas gauche) {
		this.gauche = gauche;
	}

	public void setDroite(VPIDatas droite) {
		this.droite = droite;
	}
	
	
}
