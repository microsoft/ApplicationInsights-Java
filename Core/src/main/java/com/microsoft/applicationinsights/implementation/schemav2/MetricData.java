package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;
import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

/**
 * Data contract class MetricData.
 */
public class MetricData extends Domain implements JsonSerializable {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String METRIC_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Metric";

    /**
     * Base Type for this telemetry.
     */
    public static final String METRIC_BASE_TYPE = "Microsoft.ApplicationInsights.MetricData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Metrics.
     */
    private List<DataPoint> metrics;

    /**
     * Backing field for property Properties.
     */
    private HashMap<String, String> properties;

    /**
     * Initializes a new instance of the <see cref="MetricData"/> class.
     */
    public MetricData()
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
     * Gets the Metrics property.
     */
    public List<DataPoint> getMetrics() {
        if (this.metrics == null) {
            this.metrics = new ArrayList<DataPoint>();
        }
        return this.metrics;
    }

    /**
     * Sets the Metrics property.
     */
    public void setMetrics(List<DataPoint> value) {
        this.metrics = value;
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
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("metrics", metrics);
        writer.write("properties", properties);
    }

    @Override
    public String getEnvelopName() {
        return METRIC_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return METRIC_BASE_TYPE;
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {

    }
}
