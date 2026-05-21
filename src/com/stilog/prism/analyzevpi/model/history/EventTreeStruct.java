package com.stilog.prism.analyzevpi.model.history;

import java.util.*;

/**
 * Représente un {@code EventTreeStruct} VP — un arbre de création d'événements.
 *
 * <h2>Règle d'identification</h2>
 * Un event appartient à un arbre si et seulement si :
 * <ol>
 *   <li>Toutes ses dimensions {@code mandatory=true, depth=0} sont renseignées
 *       sur l'event.</li>
 *   <li>L'event ne possède <b>aucune</b> dimension étrangère à l'arbre —
 *       c'est-à-dire aucune ressource renseignée qui ne figure ni dans les
 *       mandatory ni dans les optionnelles de l'arbre.</li>
 * </ol>
 *
 * <p>Exemple : "05. Work Order Activity" a mandatory={RM4,RM6,RM19} et
 * optionales={RM3,RM9,RM10,RM11,…}. Un event avec RM29 (TEST) est étranger
 * à cet arbre → il appartient à "Network Activity Phantom" qui exige RM29.
 */
public class EventTreeStruct {

    private final int           id;
    private final String        name;
    private final boolean       activated;
    private final List<DimEntry> dims;

    /** Ensemble de tous les rmId de l'arbre (mandatory + optionnelles). */
    private final Set<String>   allDimIds;
    /** Ensemble des rmId mandatory à depth=0 uniquement. */
    private final Set<String>   mandatoryIds;

    public EventTreeStruct(int id, String name, boolean activated, List<DimEntry> dims) {
        this.id        = id;
        this.name      = name;
        this.activated = activated;
        this.dims      = Collections.unmodifiableList(dims);

        Set<String> all  = new LinkedHashSet<>();
        Set<String> mand = new LinkedHashSet<>();
        for (DimEntry d : dims) {
            all.add(d.rmId());
            if (d.mandatory() && d.depth() == 0) mand.add(d.rmId());
        }
        this.allDimIds    = Collections.unmodifiableSet(all);
        this.mandatoryIds = Collections.unmodifiableSet(mand);
    }

    public int            getId()        { return id;        }
    public String         getName()      { return name;      }
    public boolean        isActivated()  { return activated; }
    public List<DimEntry> getDims()      { return dims;      }
    public Set<String>    getAllDimIds()  { return allDimIds; }

    /** Dimensions d'identification : mandatory=true && depth=0. */
    public List<DimEntry> getIdentificationDims() {
        return dims.stream().filter(d -> d.mandatory() && d.depth() == 0).toList();
    }

    /**
     * Vérifie si cet event appartient à cet arbre.
     *
     * <p>Règle :
     * <ol>
     *   <li>Toutes les mandatory (depth=0) sont dans {@code filledRmIds}.</li>
     *   <li>Aucun élément de {@code filledRmIds} est étranger à l'arbre
     *       (i.e. {@code filledRmIds ⊆ allDimIds}).</li>
     * </ol>
     *
     * @param filledRmIds IDs des ResourceModel renseignés sur l'event
     * @return true si l'event appartient à cet arbre
     */
    public boolean matches(Set<String> filledRmIds) {
        if (!activated || filledRmIds.isEmpty()) return false;
        // 1. Toutes les mandatory présentes
        if (!filledRmIds.containsAll(mandatoryIds)) return false;
        // 2. Aucune dimension étrangère
        return allDimIds.containsAll(filledRmIds);
    }

    /**
     * Score pour départager plusieurs arbres qui matchent tous
     * (ne devrait pas arriver si les arbres sont bien configurés).
     * Préfère l'arbre avec le plus de mandatory satisfaites.
     */
    public int matchScore(Set<String> filledRmIds) {
        if (!matches(filledRmIds)) return -1;
        return (int) filledRmIds.stream().filter(mandatoryIds::contains).count();
    }

    /**
     * @deprecated Utiliser {@link #matches(Set)} à la place.
     */
    @Deprecated
    public int compatibilityScore(Set<String> filledRmIds) {
        return matches(filledRmIds) ? matchScore(filledRmIds) : -1;
    }

    /**
     * Entrée d'une dimension dans l'arbre.
     *
     * @param rmId      identifiant du ResourceModel
     * @param mandatory vrai si obligatoire à la création
     * @param depth     0=racine (identification), ≥1=enfant (planification)
     */
    public record DimEntry(String rmId, boolean mandatory, int depth) {}
}
