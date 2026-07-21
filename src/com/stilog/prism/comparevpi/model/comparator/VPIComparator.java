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
import com.stilog.prism.vpimodel.reader.FilterConditionFormatter;
import com.stilog.prism.vpimodel.utils.VPIConstants;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;
import com.stilog.prism.vpimodel.vpsettings.Filter;
import com.visualplanning.vpi.model.filter.FilterCondition;

public class VPIComparator {

	public static void compare(ComparisonModel model) {
		VPIDatas ref = model.getData(TypeFile.COMPARISON_LEFT);
		VPIDatas tested = model.getData(TypeFile.COMPARISON_RIGHT);
		try {
			List<FileDatas> refFiles = ref.getFilesDatas();
			List<FileDatas> testedFiles = tested.getFilesDatas();
			for (FileDatas refFile : refFiles) {
				FileDatas testedFile = findByName(testedFiles, refFile.getName());
				if (testedFile == null) {
					System.out.println("Le fichier de paramétrage " + refFile.getName() + " est absent du VPI test");
					refFile.setAnomaly(true);
					continue;
				}
				compare(refFile, testedFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Apparie les FileDatas par nom plutôt que par index : {@link VPIDatas#getFilesDatas()}
	 * construit sa liste par réflexion (ordre non garanti par la JVM) et filtre les champs
	 * null (fichier de settings absent de l'archive), donc refFiles/testedFiles peuvent ne
	 * pas avoir la même taille ni le même ordre.
	 */
	private static FileDatas findByName(List<FileDatas> files, String name) {
		for (FileDatas fd : files) {
			if (fd.getName().equals(name))
				return fd;
		}
		return null;
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
					boolean equal = (isFilterFile && VPIConstants.PARAMETER_CONDITIONS.equals(attr.getKey()))
							? filterConditionsAreEqual((Filter) ref, entity, (Filter) tested, testedEntity)
							: valuesAreEqual(refValue, testedValue);

					if (!equal) {
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
	 * Compare deux valeurs d'attribut (comparaison de chaîne normalisée).
	 */
	private static boolean valuesAreEqual(String refValue, String testedValue) {
		if (refValue == null && testedValue == null) return true;
		if (refValue == null || testedValue == null) return false;
		return refValue.trim().equals(testedValue.trim());
	}

	/**
	 * Compare les conditions de deux filtres de façon sémantique, directement sur l'arbre
	 * typé de VPIReader (insensible à l'ordre des conditions, aux espaces, etc.), sans
	 * repasser par le texte affiché de l'attribut "Conditions".
	 */
	private static boolean filterConditionsAreEqual(Filter refFilter, Entity refEntity,
			Filter testedFilter, Entity testedEntity) {
		FilterCondition.LogicGroup refRoot = refFilter.getRootCondition(refEntity).orElse(null);
		FilterCondition.LogicGroup testedRoot = testedFilter.getRootCondition(testedEntity).orElse(null);
		if (refRoot == null && testedRoot == null) return true;
		if (refRoot == null || testedRoot == null) return false;

		FilterGroupNode refGroup = FilterConditionFormatter.toFilterGroupNode(refRoot);
		FilterGroupNode testedGroup = FilterConditionFormatter.toFilterGroupNode(testedRoot);
		return canonicalize(refGroup).equals(canonicalize(testedGroup));
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
