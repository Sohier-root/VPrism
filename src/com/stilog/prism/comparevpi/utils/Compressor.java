package com.stilog.prism.comparevpi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Classe utilitaire pour compresser des fichiers
 */
public class Compressor {

    /**
     * Zippe tous les fichiers d'un dossier à plat (sans structure de dossiers)
     * 
     * @param sourceFolder Le dossier source contenant les fichiers à zipper
     * @param zipFilePath Le chemin complet du fichier ZIP à créer (ex: "/tmp/archive.zip")
     * @return true si la compression a réussi, false sinon
     * @throws IOException En cas d'erreur d'entrée/sortie
     */
    private static boolean zipFolderFlat(File sourceFolder, String zipFilePath) throws IOException {
        
        // Vérifier que le dossier source existe et est bien un dossier
        if (!sourceFolder.exists()) {
            throw new IOException("Le dossier source n'existe pas : " + sourceFolder.getAbsolutePath());
        }
        
        if (!sourceFolder.isDirectory()) {
            throw new IOException("Le chemin source n'est pas un dossier : " + sourceFolder.getAbsolutePath());
        }
        
        // Créer le fichier ZIP
        File zipFile = new File(zipFilePath);
        File parentDir = zipFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        int filesCount = 0;
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // Récupérer tous les fichiers du dossier
            File[] files = sourceFolder.listFiles();
            
            if (files == null || files.length == 0) {
                System.out.println("Aucun fichier à compresser dans : " + sourceFolder.getAbsolutePath());
                return false;
            }
            
            byte[] buffer = new byte[8192];
            
            // Parcourir tous les fichiers
            for (File file : files) {
                // Ignorer les sous-dossiers, on ne traite que les fichiers
                if (file.isFile()) {
                    
                    // Créer une entrée ZIP avec juste le nom du fichier (à plat)
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);
                    
                    // Copier le contenu du fichier dans le ZIP
                    try (FileInputStream fis = new FileInputStream(file)) {
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                    }
                    
                    zos.closeEntry();
                    filesCount++;
                    
                    //System.out.println("Fichier ajouté : " + file.getName());
                }
            }
            
            System.out.println("Compression terminée : " + filesCount + " fichiers ajoutés à " + zipFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la compression : " + e.getMessage());
            throw e;
        }
        
        return filesCount > 0;
    }
    
    /**
     * Zippe tous les fichiers d'un dossier à plat avec un nom de fichier ZIP auto-généré
     * 
     * @param sourceFolder Le dossier source contenant les fichiers à zipper
     * @return Le File pointant vers le ZIP créé
     * @throws IOException En cas d'erreur d'entrée/sortie
     */
    public static File zip(File sourceFolder, File destFile) throws IOException {
        String zipFilePath = destFile.getAbsolutePath();
        zipFolderFlat(sourceFolder, zipFilePath);
        return new File(zipFilePath);
    }
}