package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;

/**
 * Data contract class ExceptionData.
 */
public class ExceptionData extends Domain implements JsonSerializable {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EXCEPTION_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Exception";

    /**
     * Base Type for this telemetry.
     */
    public static final String EXCEPTION_BASE_TYPE = "Microsoft.ApplicationInsights.ExceptionData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property HandledAt.
     */
    private String handledAt;

    /**
     * Backing field for property Exceptions.
     */
    private ArrayList<ExceptionDetails> exceptions;

    /**
     * Backing field for property SeverityLevel.
     */
    private int severityLevel;

    /**
     * Backing field for property Properties.
     */
    private HashMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private HashMap<String, Double> measurements;

    /**
     * Initializes a new instance of the <see cref="ExceptionData"/> class.
     */
    public ExceptionData()
    {
        this.InitializeFields();
    }

    /**
     * Gets the Ver property.
     */
    public int getVer() {
        return this.ver;
    }

    /**
     * Gets the HandledAt property.
     */
    public String getHandledAt() {
        return this.handledAt;
    }

    /**
     * Sets the HandledAt property.
     */
    public void setHandledAt(String value) {
        this.handledAt = value;
    }

    /**
     * Gets the Exceptions property.
     */
    public ArrayList<ExceptionDetails> getExceptions() {
        if (this.exceptions == null) {
            this.exceptions = new ArrayList<ExceptionDetails>();
        }
        return this.exceptions;
    }

    /**
     * Sets the Exceptions property.
     */
    public void setExceptions(ArrayList<ExceptionDetails> value) {
        this.exceptions = value;
    }

    /**
     * Gets the SeverityLevel property.
     */
    public int getSeverityLevel() {
        return this.severityLevel;
    }

    /**
     * Sets the SeverityLevel property.
     */
    public void setSeverityLevel(int value) {
        this.severityLevel = value;
    }

    /**
     * Gets the Properties property.
     */
    public HashMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new HashMap<String, String>();
        }
        return this.properties;
    }

    /**
     * Sets the Properties property.
     */
    public void setProperties(HashMap<String, String> value) {
        this.properties = value;
    }

    /**
     * Gets the Measurements property.
     */
    public HashMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new HashMap<String, Double>();
        }
        return this.measurements;
    }

    /**
     * Sets the Measurements property.
     */
    public void setMeasurements(HashMap<String, Double> value) {
        this.measurements = value;
    }


    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("handledAt", handledAt);
        writer.write("exceptions", exceptions);
        writer.write("measurements", measurements);
        writer.write("severityLevel", severityLevel);
        writer.write("properties", properties);
        writer.write("measurements", measurements);
    }

    @Override
    public String getEnvelopName() {
        return EXCEPTION_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return EXCEPTION_BASE_TYPE;
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
