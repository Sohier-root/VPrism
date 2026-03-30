package com.stilog.analyzemodule.controller;

import java.io.File;

import com.stilog.analyzemodule.model.SingleVPIModel;
import com.stilog.vpimodel.objects.VPIDatas;

/**
 * Contrôleur pour le Module 2 (VPI unique).
 * Orchestre le chargement du fichier et l'accès aux données.
 */
public class SingleVPIController {

    private final SingleVPIModel model;

    public SingleVPIController(SingleVPIModel model) {
        this.model = model;
    }

    /**
     * Charge un fichier VPI/VPS et retourne les données parsées.
     */
    public VPIDatas handleFile(File file) {
        model.processFile(file);
        return model.getData();
    }

    /**
     * Retourne les données courantes (sans recharger).
     */
    public VPIDatas getData() {
        return model.getData();
    }

    /**
     * Remet le modèle à zéro.
     */
    public void reset() {
        model.reset();
    }
}
