package com.stilog.vpimodel.objects;

public abstract class Resolveable {

	private boolean anomaly = false;
	private boolean resolve = false;
	
	public boolean isAnomaly() {
		return anomaly;
	}
	public void setAnomaly(boolean anomaly) {
		this.anomaly = anomaly;
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
		this.setResolve(false);
	}
	
}
