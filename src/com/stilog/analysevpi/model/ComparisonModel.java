package com.stilog.analysevpi.model;

import java.io.File;

import com.stilog.vpimodel.objects.TypeFile;
import com.stilog.vpimodel.objects.VPIDatas;

public class ComparisonModel {

	private VPIDatas gauche = new VPIDatas();
	private VPIDatas droite = new VPIDatas();
	
	public void processFile(File file, TypeFile position) {
		getData(position).computeDatas(file, position);
		if(position == TypeFile.COMPARISON_RIGHT) {
			gauche.reset();
		}
	}
	
	/*
	 * GETTER & SETTER
	 */
	public VPIDatas getData(TypeFile position) {
		switch(position) {
		case COMPARISON_LEFT:
			return gauche;
		case COMPARISON_RIGHT:
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
