package com.stilog.prism.comparevpi.controller;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import com.stilog.prism.comparevpi.model.ComparisonModel;
import com.stilog.prism.comparevpi.model.comparator.VPIComparator;
import com.stilog.prism.comparevpi.utils.AsyncDecompressor;
import com.stilog.prism.vpimodel.objects.TypeFile;
import com.stilog.prism.vpimodel.objects.VPIDatas;

public class ComparisonController {

	private ComparisonModel model;
	
	public ComparisonController(ComparisonModel model) {
		this.model = model;
	}
	
	public VPIDatas handleFile(File file, TypeFile position) {
		model.processFile(file, position);
		
		// Si c'est le fichier de droite, lancer le dézipage complet en arrière-plan
		if (position == TypeFile.COMPARISON_RIGHT) {
			startCompleteUnzip(file, position);
		}
		
		return getVPIData(position);
	}
	
	/**
	 * Lance le dézipage complet du fichier en arrière-plan (non bloquant)
	 * @param file Le fichier VPI/VPS à dézipper
	 */
	private void startCompleteUnzip(File file, TypeFile position) {
		String vpiPath = file.getAbsolutePath();
		String outputDir = System.getProperty("java.io.tmpdir") + "vpcompare/" + (position == TypeFile.COMPARISON_LEFT?"ref/":"tested/");
		
		AsyncDecompressor.dezipperAsync(
			vpiPath, 
			outputDir,
			// Callback de progression (optionnel)
			fileName -> {},
			// Callback de complétion
			() -> System.out.println("Décompression complète terminée pour : " + file.getName()),
			// Callback d'erreur
			error -> System.err.println("Erreur lors de la décompression complète : " + error.getMessage())
		);
	}
	
	/**
	 * Récupère le futur de décompression si besoin d'attendre sa complétion
	 * @param file Le fichier à dézipper
	 * @return CompletableFuture<Boolean> qui se termine quand la décompression est finie
	 */
	public CompletableFuture<Boolean> getCompleteUnzipFuture(File file, TypeFile position) {
		String vpiPath = file.getAbsolutePath();
		String outputDir = System.getProperty("java.io.tmpdir") + "vpcompare/" + (position == TypeFile.COMPARISON_LEFT?"ref/":"tested/");
		return AsyncDecompressor.dezipperAsync(vpiPath, outputDir);
	}
	
	public VPIDatas getVPIData(TypeFile position) {
		return model.getData(position);
	}
	
	public VPIDatas performComparison() {
		VPIComparator.compare(model);
		return model.getData(TypeFile.COMPARISON_LEFT);
	}
	
	public void performReverse() {
		model.reverse();
	}
}
