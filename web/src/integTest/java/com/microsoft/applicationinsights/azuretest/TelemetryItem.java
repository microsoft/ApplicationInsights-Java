package com.microsoft.applicationinsights.azuretest;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by moralt on 30/4/2015.
 */
public class TelemetryItem {
    private static Hashtable<DocumentType, ArrayList<String>> defaultPropertiesToCompare = new Hashtable<DocumentType, ArrayList<String>>();
    private DocumentType docType;
    private String id;
    private Hashtable<String, String> properties = new Hashtable<String, String>();

    /**
     * Initializes a new TelemetryItem object
     * @param docType The document type of the telemetry item
     */
    public TelemetryItem(DocumentType docType, String id) {
        this.docType = docType;
        this.id = id;
        initDefaultPropertiesToCompare();
    }

    private void initDefaultPropertiesToCompare() {
        ArrayList<String> requesrtsParameters = new ArrayList<String>() {
            {
                add("port");
                add("responseCode");
                add("uri");
            }
        };

        ArrayList<String> PerformanceCountersParameters = new ArrayList<String>() {
            {
                add("category");
                add("instance");
            }
        };

        defaultPropertiesToCompare.put(DocumentType.Requests, requesrtsParameters);
        defaultPropertiesToCompare.put(DocumentType.PerformanceCounters, PerformanceCountersParameters);
    }

    public DocumentType getDocType() {
        return this.docType;
    }

    public String getId() {
        return this.id;
    }

    /**
     * Gets a property of the telemetry item
     * @param name The property name
     * @return The property of the telemetry item
     */
    public String getProperty(String name) {
        return this.properties.get(name);
    }

    /**
     * Sets a property of the telemetry item
     * @param name The property name
     * @param value The property value
     */
    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

    /**
     * Tests if the properties of the this item equals to the properties of another telemetry item
     * @param obj The other object
     * @return True if equals, otherwise false.
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj == null || !(obj instanceof TelemetryItem)) {
            return false;
        }

        TelemetryItem telemetry = (TelemetryItem) obj;

        if(telemetry.getDocType() != this.getDocType()) {
            return false;
        }

        for (String propertyName : defaultPropertiesToCompare.get(this.getDocType())) {
            if(telemetry.getProperty(propertyName) == null && this.getProperty(propertyName) == null) {
                continue;
            }

            if(telemetry.getProperty(propertyName) == null ||
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