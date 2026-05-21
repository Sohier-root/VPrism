package com.stilog.prism.comparevpi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.stilog.prism.vpimodel.utils.VPIConstants;

public class Decompressor {

    public static void dezipper(String zipFichier, String dossierDestination) {

        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFichier))) {

            ZipEntry entree;

            File destDir = new File(dossierDestination);
            if (destDir.exists()) {
            	destDir.delete();
            }
            
            List<String> fileNames = getFilenameConstants(VPIConstants.class);
            while ((entree = zis.getNextEntry()) != null) {

                File nouveauFichier = new File(dossierDestination + entree.getName());

                if(!fileNames.contains(nouveauFichier.getName()))
                	continue;
                // Créer les dossiers si nécessaire
                if (entree.isDirectory()) {
                    nouveauFichier.mkdirs();
                    continue;
                } else {
                    new File(nouveauFichier.getParent()).mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(nouveauFichier);

                int longueur;
                while ((longueur = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, longueur);
                }

                fos.close();
                zis.closeEntry();
            }

            System.out.println("Décompression terminée !");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static List<String> getFilenameConstants(Class<?> clazz) {
        List<String> result = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {

            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && field.getType().equals(String.class)
                    && field.getName().startsWith("FILENAME_")) {

                try {
                    // Comme c'est static, on passe null
                    String value = (String) field.get(null);
                    result.add(value);

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
