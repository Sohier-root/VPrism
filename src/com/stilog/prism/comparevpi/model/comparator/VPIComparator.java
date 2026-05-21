package com.stilog.prism.comparevpi.model.comparator;

import java.util.List;

import com.stilog.prism.comparevpi.model.ComparisonModel;
import com.stilog.prism.vpimodel.objects.Attribute;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.objects.TypeFile;
import com.stilog.prism.vpimodel.objects.VPIDatas;
import com.stilog.prism.vpimodel.objects.filter.FilterGroupNode;
import com.stilog.prism.vpimodel.objects.filter.FilterLeafNode;
import com.stilog.prism.vpimodel.objects.filter.FilterNode;
import com.stilog.prism.vpimodel.utils.FilterParser;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;
import com.stilog.prism.vpimodel.vpsettings.Filter;

public class VPIComparator {

	public static void compare(ComparisonModel model) {
		VPIDatas ref = model.getData(TypeFile.COMPARISON_LEFT);
		VPIDatas tested = model.getData(TypeFile.COMPARISON_RIGHT);
		try {
			List<FileDatas> refFiles = ref.getFilesDatas();
			List<FileDatas> testedFiles = tested.getFilesDatas();
			for (int i = 0; i < refFiles.size(); i++) {
				compare(refFiles.get(i), testedFiles.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void compare(FileDatas ref, FileDatas tested) {
		// Reset de tous les états avant comparaison
		for (Entity entity : ref.getEntities()) {
			entity.setAnomaly(false);
			entity.setChanged(false);
			for (Parameters param : entity.getParameters()) {
				param.setAnomaly(false);
				param.setChanged(false);
				for (Attribute attr : param.getAttributes()) {
					attr.setAnomaly(false);
					attr.setChanged(false);
				}
			}
		}

		boolean isFilterFile = (ref instanceof Filter);

		for (Entity entity : ref.getEntities()) {
			for (Parameters param : entity.getParameters()) {

				// --- Présence de l'entité ---
				Entity testedEntity = tested.getEntity(entity.getName());
				if (testedEntity == null) {
					System.out.println("L'entity " + entity.getName() + " est absent du VPI test");
					entity.setAnomaly(true);
					ref.setAnomaly(true);
					break;
				}

				// --- Présence du paramètre ---
				List<Parameters> testedParams = testedEntity.getParameters(param.getName());
				if (testedParams.isEmpty()) {
					System.out.println("Le paramètre " + param.getName() + " est absent du VPI test");
					param.setAnomaly(true);
					entity.setAnomaly(true);
					ref.setAnomaly(true);
					continue;
				}

				// Sélection du paramètre cible (gestion des doublons par UID)
				Parameters targetParam = testedParams.get(0);
				if (param.getUid() != null && testedParams.size() > 1) {
					for (Parameters testedParam : testedParams) {
						if (testedParam.getUid() == null) continue;
						if (testedParam.getUid().equals(param.getUid())) {
							targetParam = testedParam;
							break;
						}
						if (testedParam.getAttributeValue(VPIConstants.PARAMETER_TYPE) != null
								&& testedParam.getAttributeValue(VPIConstants.PARAMETER_TYPE)
										.equals(param.getAttributeValue(VPIConstants.PARAMETER_TYPE))
								&& testedParam.getName().equals(param.getName())) {
							targetParam = testedParam;
						}
					}
				}

				// --- Comparaison des attributs ---
				for (Attribute attr : param.getAttributes()) {
					String refValue = attr.getValue();
					String testedValue = targetParam.getAttributeValue(attr.getKey());

					// Attribut absent (getAttributeValue retourne "" quand la clé n'existe pas)
					if (testedValue == null || testedValue.isEmpty()) {
						// Vérifier que c'est bien une absence et pas une valeur vide légitime
						if (!targetParam.hasAttribute(attr.getKey())) {
							System.out.println("L'attribut " + attr.getKey() + " est absent du VPI test");
							attr.setAnomaly(true);
							param.setAnomaly(true);
							entity.setAnomaly(true);
							ref.setAnomaly(true);
							continue;
						}
					}

					// Attribut présent : comparer les valeurs
					if (!valuesAreEqual(attr.getKey(), refValue, testedValue, isFilterFile)) {
						System.out.println("L'attribut " + attr.getKey()
								+ " a une valeur différente : [" + refValue + "] vs [" + testedValue + "]");
						attr.setChanged(true);
						param.setChanged(true);
						entity.setChanged(true);
						ref.setChanged(true);
					}
				}
			}
		}
	}

	/**
	 * Compare deux valeurs d'attribut.
	 * Pour l'attribut "Conditions" d'un filtre, utilise une comparaison sémantique
	 * via FilterParser (insensible à l'ordre des conditions, aux espaces, etc.).
	 * Pour tous les autres attributs, comparaison de chaîne normalisée.
	 */
	private static boolean valuesAreEqual(String key, String refValue, String testedValue,
			boolean isFilterFile) {
		if (refValue == null && testedValue == null) return true;
		if (refValue == null || testedValue == null) return false;

		// Comparaison sémantique pour les conditions de filtre
		if (isFilterFile && VPIConstants.PARAMETER_CONDITIONS.equals(key)) {
			return filterConditionsAreEqual(refValue, testedValue);
		}

		// Comparaison de chaîne normalisée pour tout le reste
		return refValue.trim().equals(testedValue.trim());
	}

	/**
	 * Compare deux XML de filterCondition de façon sémantique :
	 * parse les deux côtés en FilterGroupNode et compare leur représentation
	 * canonique (opérateur + conditions triées alphabétiquement par attribut).
	 * 
	 * On passe isEventFilter=false car on compare uniquement la structure,
	 * pas la résolution des noms via GeneralCorrespondance.
	 */
	private static boolean filterConditionsAreEqual(String refXml, String testedXml) {
		try {
			FilterGroupNode refGroup    = FilterParser.parse(refXml,    false);
			FilterGroupNode testedGroup = FilterParser.parse(testedXml, false);
			return canonicalize(refGroup).equals(canonicalize(testedGroup));
		} catch (Exception e) {
			// En cas d'erreur de parsing, repli sur comparaison de chaîne
			return refXml.trim().equals(testedXml.trim());
		}
	}

	/**
	 * Produit une représentation canonique d'un groupe de filtre :
	 * opérateur suivi des enfants triés, récursivement.
	 * Exemple : "AND[Actif=Oui][Equipe est dans 3, 1]"
	 */
	private static String canonicalize(FilterNode node) {
		if (node.isLeaf()) {
			FilterLeafNode leaf = (FilterLeafNode) node;
			return "[" + leaf.getAttributeTitle()
				 + "|" + leaf.getOperator()
				 + "|" + leaf.getValueDisplay() + "]";
		}

		FilterGroupNode group = (FilterGroupNode) node;
		List<String> parts = new java.util.ArrayList<>();
		for (FilterNode child : group.getChildren()) {
			parts.add(canonicalize(child));
		}
		java.util.Collections.sort(parts);
		return group.getOperator() + parts.toString();
	}
}
