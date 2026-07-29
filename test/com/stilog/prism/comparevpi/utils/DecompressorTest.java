package com.stilog.prism.comparevpi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Vérifie deux comportements corrigés lors de l'audit du code :
 * - une entrée zip malveillante ("../") ne doit jamais écrire hors du dossier
 *   de destination (zip-slip) ;
 * - le contenu d'un chargement précédent ne doit jamais persister lors d'un
 *   rechargement dans le même dossier (auparavant, destDir.delete() échouait
 *   silencieusement sur un dossier non vide).
 */
class DecompressorTest {

    @TempDir
    File tempDir;

    @Test
    void refuseUneEntreeZipSlip() throws IOException {
        File zip = new File(tempDir, "malicious.vpi");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("../../../../tmp/evil/resourcemodel.txt", "1;\"evil\";0;\"\"".getBytes(StandardCharsets.UTF_8));
        createZip(zip, entries);

        File destDir = new File(tempDir, "out");

        assertThrows(UncheckedIOException.class, () -> Decompressor.dezipper(zip.getAbsolutePath(), destDir.getAbsolutePath() + File.separator));

        // Rien n'a été écrit en dehors du dossier de destination
        File escaped = new File(tempDir, "evil/resourcemodel.txt").getCanonicalFile();
        assertFalse(escaped.exists(), "Un fichier a été écrit hors du dossier de destination : " + escaped);
    }

    @Test
    void supprimeLesFichiersPerimesEntreDeuxChargements() throws IOException {
        File destDir = new File(tempDir, "out");
        String destPath = destDir.getAbsolutePath() + File.separator;

        // Premier chargement : deux fichiers reconnus
        File zip1 = new File(tempDir, "riche.vpi");
        Map<String, byte[]> entries1 = new LinkedHashMap<>();
        entries1.put("resourcemodel.txt", "1;\"Dimension\";0;\"\"".getBytes(StandardCharsets.UTF_8));
        entries1.put("eventtreestruct.txt", "1;\"Arbre\";0;\"\"".getBytes(StandardCharsets.UTF_8));
        createZip(zip1, entries1);

        Decompressor.dezipper(zip1.getAbsolutePath(), destPath);
        assertTrue(new File(destDir, "resourcemodel.txt").exists());
        assertTrue(new File(destDir, "eventtreestruct.txt").exists());

        // Second chargement, même dossier : n'a plus eventtreestruct.txt
        File zip2 = new File(tempDir, "pauvre.vpi");
        Map<String, byte[]> entries2 = new LinkedHashMap<>();
        entries2.put("resourcemodel.txt", "2;\"Autre dimension\";0;\"\"".getBytes(StandardCharsets.UTF_8));
        createZip(zip2, entries2);

        Decompressor.dezipper(zip2.getAbsolutePath(), destPath);

        assertTrue(new File(destDir, "resourcemodel.txt").exists());
        assertFalse(new File(destDir, "eventtreestruct.txt").exists(),
            "Le fichier de l'archive précédente n'aurait pas dû persister");
        assertEquals("2;\"Autre dimension\";0;\"\"",
            Files.readString(new File(destDir, "resourcemodel.txt").toPath(), StandardCharsets.UTF_8));
    }

    private static void createZip(File zip, Map<String, byte[]> entries) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue());
                zos.closeEntry();
            }
            zos.finish();
            Files.write(zip.toPath(), baos.toByteArray());
        }
    }
}
