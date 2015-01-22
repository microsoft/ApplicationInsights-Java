package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class EventData.
 */
public class EventData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    private static final String EVENT_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Event";

    /**
     * Base Type for this telemetry.
     */
    private static final String EVENT_BASE_TYPE = "Microsoft.ApplicationInsights.EventData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private ConcurrentMap<String, Double> measurements;

    /**
     * Initializes a new instance of the class.
     */
    public EventData() {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentHashMap<String, String>();
        }
        return this.properties;
    }

    public void setProperties(ConcurrentMap<String, String> value) {
        this.properties = value;
    }

    public ConcurrentMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new ConcurrentHashMap<String, Double>();
        }
        return this.measurements;
    }

    public void setMeasurements(ConcurrentMap<String, Double> value) {
        this.measurements = value;
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        writer.write("ver", ver);
        writer.write("name", name);
        writer.write("properties", properties);
        writer.write("measurements", measurements);
    }

    @Override
    public String getEnvelopName() {
        return EVENT_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return EVENT_BASE_TYPE;
    }

    protected void InitializeFields() {
    }
}
