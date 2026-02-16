package com.stilog.documentation.util;

import java.io.InputStream;

import com.stilog.documentation.exception.TemplateNotFoundException;

public class DocumentUtil {
    
    /**
     * Charge une ressource depuis le classpath
     * @param resourcePath chemin de la ressource (ex: "templates/data.xlsx" ou "/templates/data.xlsx")
     * @return InputStream de la ressource
     * @throws TemplateNotFoundException si la ressource n'est pas trouvée
     */
    public static InputStream getResourceAsStream(String resourcePath) {
        // Normaliser le chemin (ajouter / au début si absent)
        String normalizedPath = resourcePath;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        
        // Essayer de charger la ressource avec la classe DocumentUtil
        InputStream inputStream = DocumentUtil.class.getResourceAsStream(normalizedPath);
        
        if (inputStream == null) {
            // Essayer avec le ClassLoader (sans le / initial)
            inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(normalizedPath.substring(1));
        }
        
        if (inputStream == null) {
            throw new TemplateNotFoundException(
                "Ressource non trouvée dans le classpath: " + resourcePath);
        }
        
        return inputStream;
    }
    
    /**
     * Vérifie si une ressource existe dans le classpath
     * @param resourcePath chemin de la ressource
     * @return true si la ressource existe, false sinon
     */
    public static boolean resourceExists(String resourcePath) {
        try {
            InputStream is = getResourceAsStream(resourcePath);
            is.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
