package com.stilog.analysevpi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service de décompression asynchrone pour gérer le dézipage en arrière-plan
 */
public class AsyncDecompressor {
    
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("AsyncDecompressor-" + thread.getId());
        return thread;
    });
    
    /**
     * Dézippe un fichier de manière asynchrone (tous les fichiers)
     * 
     * @param zipFichier Chemin du fichier ZIP à décompresser
     * @param dossierDestination Dossier de destination
     * @param onProgress Callback optionnel pour suivre la progression (fichier en cours)
     * @param onComplete Callback optionnel appelé à la fin de la décompression
     * @param onError Callback optionnel en cas d'erreur
     * @return CompletableFuture<Boolean> qui se complète quand la décompression est terminée
     */
    public static CompletableFuture<Boolean> dezipperAsync(
            String zipFichier, 
            String dossierDestination,
            Consumer<String> onProgress,
            Runnable onComplete,
            Consumer<Exception> onError) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                dezipperComplet(zipFichier, dossierDestination, onProgress);
                
                if (onComplete != null) {
                    onComplete.run();
                }
                
                return true;
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e);
                }
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * Dézippe un fichier de manière asynchrone (version simplifiée)
     * 
     * @param zipFichier Chemin du fichier ZIP à décompresser
     * @param dossierDestination Dossier de destination
     * @return CompletableFuture<Boolean> qui se complète quand la décompression est terminée
     */
    public static CompletableFuture<Boolean> dezipperAsync(String zipFichier, String dossierDestination) {
        return dezipperAsync(zipFichier, dossierDestination, null, null, null);
    }
    
    /**
     * Décompression complète (tous les fichiers) - méthode synchrone
     * 
     * @param zipFichier Chemin du fichier ZIP
     * @param dossierDestination Dossier de destination
     * @param onProgress Callback optionnel pour la progression
     * @throws IOException En cas d'erreur de lecture/écriture
     */
    private static void dezipperComplet(String zipFichier, String dossierDestination, Consumer<String> onProgress) throws IOException {
        byte[] buffer = new byte[8192]; // Buffer plus grand pour meilleures performances
        
        File destDir = new File(dossierDestination);
        if (destDir.exists()) {
            destDir.mkdirs();
        }
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFichier))) {
            ZipEntry entree;
            int fileCount = 0;
            
            while ((entree = zis.getNextEntry()) != null) {
                File nouveauFichier = new File(dossierDestination + File.separator + entree.getName());
                
                // Sécurité : vérifier que le fichier reste dans le dossier de destination
                String destDirPath = destDir.getCanonicalPath();
                String destFilePath = nouveauFichier.getCanonicalPath();
                
                if (!destFilePath.startsWith(destDirPath + File.separator)) {
                    throw new IOException("Entrée en dehors du répertoire cible : " + entree.getName());
                }
                
                if (entree.isDirectory()) {
                    nouveauFichier.mkdirs();
                } else {
                    // Créer les dossiers parents si nécessaire
                    File parent = nouveauFichier.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    
                    // Notifier la progression
                    if (onProgress != null) {
                        onProgress.accept(entree.getName());
                    }
                    
                    // Écrire le fichier
                    try (FileOutputStream fos = new FileOutputStream(nouveauFichier)) {
                        int longueur;
                        while ((longueur = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, longueur);
                        }
                    }
                    
                    fileCount++;
                }
                
                zis.closeEntry();
            }
            
            System.out.println("Décompression complète terminée : " + fileCount + " fichiers extraits");
            System.out.println("Path : " + dossierDestination);
        }
    }
    
    /**
     * Arrête proprement le service de décompression
     * À appeler lors de la fermeture de l'application
     */
    public static void shutdown() {
        executor.shutdown();
    }
}
