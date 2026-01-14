package com.stilog.analysevpi.model.comparator;

import java.lang.reflect.Field;
import java.util.List;

import com.stilog.analysevpi.model.Model;
import com.stilog.analysevpi.model.objects.Attribute;
import com.stilog.analysevpi.model.objects.Entity;
import com.stilog.analysevpi.model.objects.Parameters;
import com.stilog.analysevpi.model.objects.PositionFile;
import com.stilog.analysevpi.model.objects.VPIDatas;
import com.stilog.analysevpi.model.vpsettings.FileDatas;
import com.stilog.analysevpi.utils.VPIConstants;

public class VPIComparator {

	public static void compare(Model model) {
		VPIDatas ref = model.getData(PositionFile.LEFT);
		VPIDatas tested = model.getData(PositionFile.RIGHT);
		try {
			/*
			 * Scan de toutes les données à comparer
			 */
			List<Field> refFiles = ref.getFilesDatas();
			List<Field> testedFiles = tested.getFilesDatas();
			for(int i = 0; i<refFiles.size(); i++) {
				compare((FileDatas) refFiles.get(i).get(ref), (FileDatas) testedFiles.get(i).get(tested));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void compare(FileDatas ref, FileDatas tested) {
		for(Entity entity : ref.getEntities()) {
			entity.setAnomaly(false);
			for(Parameters param : entity.getParameters()) {
				param.setAnomaly(false);
			}
		}
		
		for(Entity entity : ref.getEntities()) {
			for(Parameters param : entity.getParameters()) {
				//Présence de l'entité
				Entity testedEntity;
				if((testedEntity = tested.getEntity(entity.getName())) == null) {
					System.out.println("L'entity " + entity.getName() + " est absent du VPI test");
					entity.setAnomaly(true);
					ref.setAnomaly(true);
					break;
				}
				
				//Présence du paramètre
				List<Parameters> testedParams;
				Parameters targetParam;
				if((testedParams = testedEntity.getParameters(param.getName())).isEmpty()) {
					System.out.println("Le paramètre " + param.getName() + " est absent du VPI test");
					param.setAnomaly(true);
					entity.setAnomaly(true);
					ref.setAnomaly(true);
					continue;
				}
				else {
					//Récupération du paramètre en cause (en cas de plusieurs avec le même nom)
					targetParam = testedParams.get(0);
					if(param.getUid() != null && testedParams.size() > 1) {
						boolean isFound = false;
						for(Parameters testedParam : testedParams) {
							if(testedParam.getUid() == null)
								continue;
							
							if(testedParam.getUid().equals(param.getUid())) {
								targetParam = testedParam;
								isFound = true;
								break;
							}
							
							if(testedParam.getAttributeValue(VPIConstants.PARAMETER_TYPE) != null 
								&& testedParam.getAttributeValue(VPIConstants.PARAMETER_TYPE).equals(param.getAttributeValue(VPIConstants.PARAMETER_TYPE))
								&& testedParam.getName().equals(param.getName())) {
								targetParam = testedParam;
							}
						}
					}
				}
				
				//Comparaison Attributs
				for(Attribute attr : param.getAttributes()) {
					if(targetParam.getAttributeValue(attr.getKey()) == null) {
						System.out.println("L'attribut " + attr.getKey() + " est absent du VPI test");
						attr.setAnomaly(true);
						param.setAnomaly(true);
						entity.setAnomaly(true);
						ref.setAnomaly(true);
					}
				}
			}
		}
	}
}
