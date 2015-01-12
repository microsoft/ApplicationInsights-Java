package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

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
    private ConcurrentMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private ConcurrentMap<String, Double> measurements;

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
    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentMap<String, String>() {
                @Override
                public String putIfAbsent(String key, String value) {
                    return null;
                }

                @Override
                public boolean remove(Object key, Object value) {
                    return false;
                }

                @Override
                public boolean replace(String key, String oldValue, String newValue) {
                    return false;
                }

                @Override
                public String replace(String key, String value) {
                    return null;
                }

                @Override
                public int size() {
                    return 0;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public boolean containsKey(Object key) {
                    return false;
                }

                @Override
                public boolean containsValue(Object value) {
                    return false;
                }

                @Override
                public String get(Object key) {
                    return null;
                }

                @Override
                public String put(String key, String value) {
                    return null;
                }

                @Override
                public String remove(Object key) {
                    return null;
                }

                @Override
                public void putAll(Map<? extends String, ? extends String> m) {

                }

                @Override
                public void clear() {

                }

                @Override
                public Set<String> keySet() {
                    return null;
                }

                @Override
                public Collection<String> values() {
                    return null;
                }

                @Override
                public Set<Entry<String, String>> entrySet() {
                    return null;
                }
            };
        }
        return this.properties;
    }

    /**
     * Sets the Properties property.
     */
    public void setProperties(ConcurrentMap<String, String> value) {
        this.properties = value;
    }

    /**
     * Gets the Measurements property.
     */
    public ConcurrentMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new ConcurrentHashMap<String, Double>();
        }
        return this.measurements;
    }

    /**
     * Sets the Measurements property.
     */
    public void setMeasurements(ConcurrentMap<String, Double> value) {
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
