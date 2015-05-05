package com.microsoft.applicationinsights.springwebapptest;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by moralt on 05/05/2015.
 */
public abstract class TelemetryItem extends Properties{
    protected static ArrayList<String> defaultPropertiesToCompare;
    private DocumentType docType;
    private String id;

    /**
     * Initializes a new TelemetryItem object
     * @param docType The document type of the telemetry item
     * @param id The ID of this object
     */
    public TelemetryItem(DocumentType docType, String id) {
        this.docType = docType;
        this.id = id;
        initDefaultPropertiesToCompare();
    }

    protected abstract void initDefaultPropertiesToCompare();

    public DocumentType getDocType() {
        return this.docType;
    }

    public String getId() {
        return this.id;
    }

    /**
     * Tests if the properties of the this item equals to the properties of another telemetry item
     * @param obj The other object
     * @return True if equals, otherwise false.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof TelemetryItem)) {
            return false;
        }

        TelemetryItem telemetry = (TelemetryItem) obj;

        if (telemetry.getDocType() != this.getDocType()) {
            return false;
        }

        for (String propertyName : defaultPropertiesToCompare) {
            if (telemetry.getProperty(propertyName) == null && this.getProperty(propertyName) == null) {
                continue;
            }

            if (telemetry.getProperty(propertyName) == null ||
                    this.getProperty(propertyName) == null ||
                    !telemetry.getProperty(propertyName).equalsIgnoreCase(this.getProperty(propertyName))) {
                System.out.println("Mismatch for property name '" + propertyName + "': '" + telemetry.getProperty(propertyName) + "' '" + getProperty(propertyName) + "'.");
                return false;
            }
        }

        return true;
    }



    /**
     * Returns the Hashcode of the ID of this object
     * @return The Hashcode of the ID of this object
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
