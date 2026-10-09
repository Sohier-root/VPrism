package com.stilog.prism.vpimodel.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.stilog.prism.analyzevpi.model.history.VpsLabelResolver;
import com.stilog.prism.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.prism.vpimodel.objects.filter.FilterLeafNode;
import com.visualplanning.vpi.model.VpiArchive;
import com.visualplanning.vpi.model.VpiPlanning;
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

    /**
     * Cas réel "Plant est dans 1" (filtre Work Center) : l'attribut "Plant" est porté par
     * la dimension Work Center, mais référence la dimension Plant. Le XML de la condition
     * (&lt;resourceModel&gt;&lt;entityID&gt;) y vaut l'ID de la dimension PROPRIÉTAIRE
     * (Work Center), pas celui de Plant — il faut retrouver Plant via le heading
     * ResourceReference "Plant" déclaré sur Work Center (resolveTargetDimensionId).
     */
    @Test
    void resoutLaDimensionReferenceeQuandElleDiffereDeLaDimensionProprietaire() throws IOException {
        VpsLabelResolver resolver = loadMinimalResolver(7, "Plant Alpha", "Plant Beta");
        VpiPlanning planning = loadPlanningAvecWorkCenterEtPlant();

        // attributeId=5 (heading "Plant" sur Work Center id=3), resourceModelEntityId=3
        // (confond ici avec le propriétaire, comme observé en réel) au lieu de 7 (Plant).
        FilterAttribute attribute = new FilterAttribute(5, "Plant", 3, -1);
        FilterCondition.ResourceAttributeCondition condition = new FilterCondition.ResourceAttributeCondition(
            attribute, 3, "IN", true, "1", true, false, false, "");

        FilterGroupNode group = FilterConditionFormatter.toFilterGroupNode(condition, resolver, planning);
        FilterLeafNode leaf = (FilterLeafNode) group.getChildren().get(0);

        assertEquals("Plant Alpha", leaf.getValueDisplay());
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
        return loadMinimalResolver(1, "Equipe Alpha", "Equipe Beta", "Equipe Gamma");
    }

    private VpsLabelResolver loadMinimalResolver(int tableId, String... names) throws IOException {
        File vps = new File(tempDir, "sample-" + tableId + ".vps");
        writeMinimalVpsArchive(vps, tableId, names);
        VpsLabelResolver resolver = new VpsLabelResolver();
        resolver.load(vps);
        return resolver;
    }

    /**
     * Archive VPS minimale : tables.xml (schéma d'une seule colonne texte pour
     * eventresource&lt;tableId&gt;) + eventresource&lt;tableId&gt;.txt (ressources nommées).
     * Suffisant pour que VpsLabelResolver résolve "eventresource<tableId>|<id>" -> nom, sans
     * avoir besoin de resourcemodel.txt (VpsLabelResolver retombe sur les colonnes texte
     * détectées dans tables.xml quand aucun keyHeading n'est déclaré).
     */
    private void writeMinimalVpsArchive(File vps, int tableId, String... names) throws IOException {
        String table = "eventresource" + tableId;
        String tablesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<tables>"
            + "<SQLT><name>" + table + "</name>"
            + "<SQLC><name>ID</name><sqlType>4</sqlType></SQLC>"
            + "<SQLC><name>UID</name><sqlType>1</sqlType></SQLC>"
            + "<SQLC><name>rub1</name><sqlType>1</sqlType></SQLC>"
            + "</SQLT></tables>";

        StringBuilder resourceData = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            resourceData.append(i + 1).append(";\"UID-").append(i + 1).append("\";\"").append(names[i]).append("\"\n");
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(vps.toPath()))) {
            zos.putNextEntry(new ZipEntry("tables.xml"));
            zos.write(tablesXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry(table + ".txt"));
            zos.write(resourceData.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /**
     * Construit un VpiPlanning avec deux dimensions : Work Center (id=3), qui porte un
     * heading ResourceReference "Plant" (id=5) référençant la dimension Plant (id=7, sans
     * heading propre, inutile ici). Reproduit la structure réelle qui fait que
     * resourceModelEntityId() de la condition vaut l'ID du propriétaire (3) et non celui
     * de la cible (7).
     */
    private VpiPlanning loadPlanningAvecWorkCenterEtPlant() throws IOException {
        String workCenterXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<com.visualplanning.data.resource.ResourceModel>"
            + "<ID>3</ID><UID>WC-UID</UID><comments></comments>"
            + "<headings>"
            + "<com.visualplanning.data.Heading>"
            + "<ID>5</ID><UID>H-UID</UID><name>Plant</name><ownerID>0</ownerID><comments></comments>"
            + "<type class=\"ResourceReference\"><resourceModel><entityID>7</entityID></resourceModel></type>"
            + "</com.visualplanning.data.Heading>"
            + "</headings>"
            + "</com.visualplanning.data.resource.ResourceModel>";
        String plantXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<com.visualplanning.data.resource.ResourceModel>"
            + "<ID>7</ID><UID>PLANT-UID</UID><comments></comments>"
            + "</com.visualplanning.data.resource.ResourceModel>";

        String resourceModelTxt =
            "3;\"Work Center\";0;\"" + b64(workCenterXml) + "\"\n"
            + "7;\"Plant\";0;\"" + b64(plantXml) + "\"\n";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry("resourcemodel.txt"));
            zos.write(resourceModelTxt.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("formmodel.txt"));
            zos.closeEntry();
        }

        return VpiPlanning.parse(VpiArchive.read(new ByteArrayInputStream(bos.toByteArray())));
    }

    private static String b64(String xml) {
        return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
    }
}
