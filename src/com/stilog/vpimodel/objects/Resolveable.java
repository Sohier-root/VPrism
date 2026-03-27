package com.stilog.vpimodel.objects;

public abstract class Resolveable {

	private boolean anomaly = false;  // élément absent dans le VPI testé
	private boolean changed = false;  // élément présent mais valeur différente
	private boolean resolve = false;  // anomalie résolue par merge/replace
	
	public boolean isAnomaly() {
		return anomaly;
	}
	public void setAnomaly(boolean anomaly) {
		this.anomaly = anomaly;
	}

	public boolean isChanged() {
		return changed;
	}
	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public boolean isResolve() {
		return resolve;
	}
	public void setResolve(boolean resolve) {
		this.resolve = resolve;
	}
	
	/**
	 * Remet les données de comparaison a zéro
	 */
	public void reset() {
		this.setAnomaly(false);
		this.setChanged(false);
		this.setResolve(false);
	}
	
}
