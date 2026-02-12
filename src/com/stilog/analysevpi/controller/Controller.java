package com.stilog.analysevpi.controller;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import com.stilog.analysevpi.model.Model;
import com.stilog.analysevpi.model.comparator.VPIComparator;
import com.stilog.analysevpi.model.dto.MergeRequest;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.model.objects.PositionFile;
import com.stilog.analysevpi.model.objects.VPIDatas;
import com.stilog.analysevpi.model.vpsettings.FileDatas;
import com.stilog.analysevpi.utils.AsyncDecompressor;

public class Controller {

	private Model model;
	
	public Controller(Model model) {
		this.model = model;
	}
	
	public VPIDatas handleFile(File file, PositionFile position) {
		model.processFile(file, position);
		
		// Si c'est le fichier de droite, lancer le dézipage complet en arrière-plan
		if (position == PositionFile.RIGHT) {
			startCompleteUnzip(file, position);
		}
		
		return getVPIData(position);
	}
	
	/**
	 * Lance le dézipage complet du fichier en arrière-plan (non bloquant)
	 * @param file Le fichier VPI/VPS à dézipper
	 */
	private void startCompleteUnzip(File file, PositionFile position) {
		String vpiPath = file.getAbsolutePath();
		String outputDir = System.getProperty("java.io.tmpdir") + "vpcompare/" + (position == PositionFile.LEFT?"ref/":"tested/");
		
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
	public CompletableFuture<Boolean> getCompleteUnzipFuture(File file, PositionFile position) {
		String vpiPath = file.getAbsolutePath();
		String outputDir = System.getProperty("java.io.tmpdir") + "vpcompare/" + (position == PositionFile.LEFT?"ref/":"tested/");
		return AsyncDecompressor.dezipperAsync(vpiPath, outputDir);
	}
	
	public VPIDatas getVPIData(PositionFile position) {
		return model.getData(position);
	}
	
	public VPIDatas performComparison() {
		VPIComparator.compare(model);
		return model.getData(PositionFile.LEFT);
	}
	
	public void performReverse() {
		model.reverse();
	}
	
	/**
	 * Méthode unifiée pour gérer toutes les opérations de merge
	 * Retourne les données mises à jour
	 * @throws Exception 
	 */
	public VPIDatas performMerge(MergeRequest request) throws Exception {
	    switch (request.getType()) {
	        case MERGE_PARAMETER:
	            return mergeParameter(
	                request.getFileData(),
	                request.getEntity(),
	                request.getParameter(),
	                null
	            );
	            
	        case MERGE_ENTITY:
	            return mergeEntity(
	                request.getFileData(),
	                request.getEntity(),
	                null
	            );
	            
	        case REPLACE_PARAMETER:
	            return mergeParameter(
	                request.getFileData(),
	                request.getEntity(),
	                request.getParameter(),
	                (Parameters) request.getTargetObject()
	            );
	            
	        case REPLACE_ENTITY:
	            return mergeEntity(
	                request.getFileData(),
	                request.getEntity(),
	                (Entity) request.getTargetObject()
	            );
	            
	        default:
	            throw new IllegalArgumentException("Type de merge non supporté: " + request.getType());
	    }
	}
	
	private VPIDatas mergeParameter(FileDatas file, Entity entity, Parameters parameter,
			Parameters parameterToReplace) throws Exception {
		VPIDatas rightDatas = model.getData(PositionFile.RIGHT);

		if (rightDatas == null) {
			throw new IllegalStateException("Aucune donnée RIGHT disponible pour le merge");
		}

		rightDatas.mergeParameter(file, entity, parameter, parameterToReplace);
		return rightDatas;
	}

	private VPIDatas mergeEntity(FileDatas file, Entity entity, Entity entityToReplace) throws Exception {
		VPIDatas rightDatas = model.getData(PositionFile.RIGHT);

		if (rightDatas == null) {
			throw new IllegalStateException("Aucune donnée RIGHT disponible pour le merge");
		}

		rightDatas.mergeEntity(file, entity, entityToReplace);
		return rightDatas;
	}

	public void performGenerateVPI(String outputPath) {
		this.model.getData(PositionFile.RIGHT).generateMergedFiles(outputPath);
	}
}
