package com.stilog.prism.analyzevpi.model;

import java.io.File;

import com.stilog.prism.vpimodel.objects.TypeFile;
import com.stilog.prism.vpimodel.objects.VPIDatas;

/**
 * Modèle pour un VPI/VPS unique (sans comparaison).
 * Contient une seule instance de VPIDatas chargée via TypeFile.SINGLE.
 */
public class SingleVPIModel {

    private VPIDatas data = new VPIDatas();

    public void processFile(File file) {
        data.computeDatas(file, TypeFile.SINGLE);
    }

    public VPIDatas getData() {
        return data;
    }

    public void reset() {
        this.data = new VPIDatas();
    }
}
