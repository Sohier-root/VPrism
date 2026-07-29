package com.stilog.prism.vpimodel.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Contrat de base du modèle générique Entity/Parameters/Attribute, utilisé partout dans la comparaison. */
class EntityParametersTest {

    @Test
    void parametersRetrouveUnAttributParSaCle() {
        Parameters param = new Parameters("Correspondances");
        param.addAttributes("Nom", "Dimension 1");
        param.addAttributes("Type", "Texte");

        assertEquals("Dimension 1", param.getAttributeValue("Nom"));
        assertEquals("Texte", param.getAttributeValue("Type"));
        assertEquals("", param.getAttributeValue("Inconnu"));
    }

    @Test
    void hasAttributeDistingueAbsenceEtValeurVide() {
        Parameters param = new Parameters("Source");
        param.addAttributes("Encodage", "");

        assertTrue(param.hasAttribute("Encodage"), "La clé existe même si sa valeur est vide");
        assertFalse(param.hasAttribute("Absent"));
    }

    @Test
    void getAttributeValueRetombeSurLesAttributsCaches() {
        Parameters param = new Parameters("Dimension 1");
        param.addHiddenAttributes("Calendrier", "5/7j France");

        assertEquals("5/7j France", param.getAttributeValue("Calendrier"));
        assertTrue(param.hasAttribute("Calendrier"));
    }

    @Test
    void entityRetrouveSesParametresParNom() {
        Entity entity = new Entity(1, "Dimension 1");
        entity.addParameter(new Parameters("Nom"));
        entity.addParameter(new Parameters("Cle"));

        assertEquals("Nom", entity.getParameter("Nom").getName());
        assertNull(entity.getParameter("Inexistant"));
        assertEquals(2, entity.getParameters().size());
    }

    @Test
    void entityGetParametersParNomRenvoieTousLesDoublons() {
        Entity entity = new Entity(1, "Formulaire");
        entity.addParameter(new Parameters("Rubrique"));
        entity.addParameter(new Parameters("Rubrique"));
        entity.addParameter(new Parameters("Autre"));

        List<Parameters> rubriques = entity.getParameters("Rubrique");
        assertEquals(2, rubriques.size());
    }

    @Test
    void entityHiddenAttributeValueParDefautEstChaineVide() {
        Entity entity = new Entity(1, "Dimension 1");
        entity.addHiddenAttributes("Commentaires", "RAS");

        assertEquals("RAS", entity.getHiddenAttributeValue("Commentaires"));
        assertEquals("", entity.getHiddenAttributeValue("Absent"));
    }

    @Test
    void entityEqualsEtHashCodeSeBasentSurLeNom() {
        Entity a = new Entity(1, "Dimension 1");
        Entity b = new Entity(2, "Dimension 1");
        Entity c = new Entity(1, "Dimension 2");

        assertEquals(a, b, "Deux entités de même nom sont égales, même avec un id différent");
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
    }
}
