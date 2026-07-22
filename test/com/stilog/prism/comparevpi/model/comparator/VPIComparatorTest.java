package com.stilog.prism.comparevpi.model.comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.stilog.prism.comparevpi.model.ComparisonModel;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.TypeFile;
import com.stilog.prism.vpimodel.objects.VPIDatas;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;

/**
 * Régression du bug critique relevé lors de l'audit : VPIComparator.compare(ComparisonModel)
 * appariait refFiles/testedFiles par index de liste, alors que VPIDatas.getFilesDatas() les
 * construit par réflexion (ordre non garanti) et exclut les fichiers de settings absents d'une
 * archive. Deux VPI n'ayant pas le même sous-ensemble de fichiers pouvaient donc voir comparés
 * les mauvais types de fichiers entre eux, ou lever une IndexOutOfBoundsException silencieusement
 * avalée. Corrigé par un appariement par nom (voir VPIComparator.findByName).
 *
 * <p>Planning_20260605_1634.vps (riche : filtres, imports/exports, hiérarchie) et
 * Planning_20260522_1726.vpi (pauvre : ces settings sont absents de l'archive) sont exactement
 * le couple qui exerçait ce bug avant correction.
 */
class VPIComparatorTest {

    private static File sample(String name) {
        try {
            URL url = VPIComparatorTest.class.getResource("/" + name);
            if (url == null)
                throw new IllegalStateException("Fixture de test introuvable sur le classpath : " + name);
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void selfCompareNeRemonteAucuneAnomalie() {
        File file = sample("Planning_20260605_1634.vps");

        ComparisonModel model = new ComparisonModel();
        model.processFile(file, TypeFile.COMPARISON_LEFT);
        model.processFile(file, TypeFile.COMPARISON_RIGHT);

        VPIComparator.compare(model);

        VPIDatas ref = model.getData(TypeFile.COMPARISON_LEFT);
        assertEquals(0, countAnomalies(ref), "Comparer un fichier avec lui-même ne doit produire aucune anomalie");
    }

    @Test
    void compareDesFichiersAuxSettingsDifferentsDetecteLesEntitesAbsentes() {
        // ref = riche (a des filtres/imports/exports/hiérarchie), tested = pauvre (n'en a pas)
        File ref = sample("Planning_20260605_1634.vps");
        File tested = sample("Planning_20260522_1726.vpi");

        ComparisonModel model = new ComparisonModel();
        model.processFile(ref, TypeFile.COMPARISON_LEFT);
        model.processFile(tested, TypeFile.COMPARISON_RIGHT);

        VPIComparator.compare(model);

        VPIDatas refData = model.getData(TypeFile.COMPARISON_LEFT);

        // Avant la correction, cet appariement par index pouvait comparer le mauvais type de
        // fichier (ex. Filtres vs Imports), ou lever une exception muette laissant 0 anomalie.
        // Valeur figée par rapport aux fichiers d'exemple réels (constatée manuellement pendant
        // l'audit) : 8 entités entièrement absentes (filtres/imports/exports/hiérarchie, absents
        // de l'archive pauvre) + 2 entités "Resources Model" avec un attribut de clé différent
        // (différence réelle de configuration entre les deux fichiers, pas un artefact du bug).
        assertEquals(10, countAnomalies(refData));

        // Chaque fichier de settings absent côté testé doit être signalé en anomalie au niveau
        // fichier — preuve que l'appariement par nom (et non par index) a bien fonctionné pour
        // CHAQUE catégorie, sans qu'aucune ne soit ignorée ou comparée à la mauvaise catégorie.
        assertTrue(refData.getResourceFilter().isAnomaly(), "Resources Filter");
        assertTrue(refData.getEventFilter().isAnomaly(), "Events Filter");

        java.util.Set<String> categoriesAttendues = java.util.Set.of(
            "Resources Export", "Events Export", "Resources Import", "Events Import", "Hierarchies");
        java.util.Set<String> categoriesVues = new java.util.HashSet<>();
        for (FileDatas fd : refData.getFilesDatas()) {
            if (categoriesAttendues.contains(fd.getName())) {
                assertTrue(fd.isAnomaly(), fd.getName() + " devrait être en anomalie (absent du VPI testé)");
                categoriesVues.add(fd.getName());
            }
        }
        assertEquals(categoriesAttendues, categoriesVues, "Toutes les catégories attendues doivent avoir été comparées");
    }

    private static int countAnomalies(VPIDatas data) {
        int count = 0;
        for (FileDatas fd : data.getFilesDatas()) {
            for (Entity e : fd.getEntities()) {
                if (e.isAnomaly()) count++;
            }
        }
        return count;
    }
}
