package com.stilog.prism.vpimodel.reader;

import java.io.File;

import com.visualplanning.vpi.exception.VpiException;
import com.visualplanning.vpi.model.VpiPlanning;

/**
 * Point d'entrée unique vers la librairie VPIReader : parse une archive
 * VPI/VPS et restitue le graphe d'objets typés déjà résolu (dimensions,
 * filtres, formulaires, hiérarchies, calendriers, import/export).
 */
public class VpiReaderService {

	private VpiReaderService() {
	}

	public static VpiPlanning parse(File vpi) throws VpiException {
		return VpiPlanning.parse(vpi.toPath());
	}
}
