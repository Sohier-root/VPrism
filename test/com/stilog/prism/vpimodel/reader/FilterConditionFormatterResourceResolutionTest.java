package com.stilog.prism.vpimodel.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.stilog.prism.analyzevpi.model.history.VpsLabelResolver;
import com.stilog.prism.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.prism.vpimodel.objects.filter.FilterLeafNode;
import com.visualplanning.vpi.model.filter.FilterAttribute;
import com.visualplanning.vpi.model.filter.FilterCondition;

/**
 * Les conditions de filtre "liste de ressources" (IN sur plusieurs ID, ex. "Equipe
 * est dans 3, 1") affichaient les ID bruts au lieu des noms des ressources
 * référencées — limitation acceptée pendant la migration VPIReader, faute de
 * résolution structurée côté reader (voir FilterConditionFormatter). Corrigé en
 * réutilisant VpsLabelResolver (déjà utilisé par l'onglet Historique) à la demande.
 */
class FilterConditionFormatterResourceResolutionTest {

    @TempDir
    File tempDir;

    @Test
    void resoutLesIdDeRessourcesEnNomsQuandLeResolveurEstFourni() throws IOException {
        VpsLabelResolver resolver = loadMinimalResolver();

        FilterCondition.ResourceAttributeCondition condition = listCondition("1, 3");

        FilterGroupNode group = FilterConditionFormatter.toFilterGroupNode(condition, resolver);
        FilterLeafNode leaf = (FilterLeafNode) group.getChildren().get(0);

        assertEquals("Equipe Alpha, Equipe Gamma", leaf.getValueDisplay());
    }

    @Test
    void repliSurLIdBrutQuandUnIdEstInconnu() throws IOException {
        VpsLabelResolver resolver = loadMinimalResolver();

        // id=99 n'existe pas dans la dimension : reste affiché tel quel, le reste résolu.
        FilterCondition.ResourceAttributeCondition condition = listCondition("1, 99");

        FilterGroupNode group = FilterConditionFormatter.toFilterGroupNode(condition, resolver);
        FilterLeafNode leaf = (FilterLeafNode) group.getChildren().get(0);

        assertEquals("Equipe Alpha, 99", leaf.getValueDisplay());
    }

    @Test
    void repliSurLIdBrutSansResolveur() {
        FilterCondition.ResourceAttributeCondition condition = listCondition("1, 3");

        FilterGroupNode group = FilterConditionFormatter.toFilterGroupNode(condition, null);
        FilterLeafNode leaf = (FilterLeafNode) group.getChildren().get(0);

        assertEquals("1, 3", leaf.getValueDisplay());
    }

    private static FilterCondition.ResourceAttributeCondition listCondition(String value) {
        FilterAttribute attribute = new FilterAttribute(0, "Equipe", 2, -1);
        return new FilterCondition.ResourceAttributeCondition(
            attribute, 1, "IN", true, value, true, false, false, "");
    }

    private VpsLabelResolver loadMinimalResolver() throws IOException {
        File vps = new File(tempDir, "sample.vps");
        writeMinimalVpsArchive(vps);
        VpsLabelResolver resolver = new VpsLabelResolver();
        resolver.load(vps);
        return resolver;
    }

    /**
     * Archive VPS minimale : tables.xml (schéma d'une seule colonne texte pour
     * eventresource1) + eventresource1.txt (3 ressources nommées). Suffisant pour
     * que VpsLabelResolver résolve "eventresource1|<id>" -> nom, sans avoir besoin
     * de resourcemodel.txt (VpsLabelResolver retombe sur les colonnes texte détectées
     * dans tables.xml quand aucun keyHeading n'est déclaré).
     */
    private void writeMinimalVpsArchive(File vps) throws IOException {
        String tablesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<tables>"
            + "<SQLT><name>eventresource1</name>"
            + "<SQLC><name>ID</name><sqlType>4</sqlType></SQLC>"
            + "<SQLC><name>UID</name><sqlType>1</sqlType></SQLC>"
            + "<SQLC><name>rub1</name><sqlType>1</sqlType></SQLC>"
            + "</SQLT></tables>";

        String resourceData =
            "1;\"UID-1\";\"Equipe Alpha\"\n"
            + "2;\"UID-2\";\"Equipe Beta\"\n"
            + "3;\"UID-3\";\"Equipe Gamma\"\n";

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(vps.toPath()))) {
            zos.putNextEntry(new ZipEntry("tables.xml"));
            zos.write(tablesXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("eventresource1.txt"));
            zos.write(resourceData.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }
}
