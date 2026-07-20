package com.stilog.prism.analyzevpi.model.history;

/**
 * Représente un élément {@code <PropertyTracker>} extrait du XML base64
 * d'une ligne {@code historytracker.txt}.
 */
public class PropertyChange {

    private final String propertyName;
    private final String oldValue;
    private final String newValue;

    public PropertyChange(String propertyName, String oldValue, String newValue) {
        this.propertyName = propertyName;
        this.oldValue     = oldValue;
        this.newValue     = newValue;
    }

    public String getPropertyName() { return propertyName; }
    public String getOldValue()     { return oldValue; }
    public String getNewValue()     { return newValue; }

    /** Vrai si la valeur a réellement changé (old ≠ new). */
    public boolean hasChanged() {
        return !java.util.Objects.equals(oldValue, newValue);
    }

    @Override
    public String toString() {
        return propertyName + " : «" + oldValue + "» → «" + newValue + "»";
    }
}
