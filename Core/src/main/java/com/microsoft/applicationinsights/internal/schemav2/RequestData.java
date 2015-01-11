package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

/**
 * Data contract class RequestData.
 */
public class RequestData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String REQUEST_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Request";

    /**
     * Base Type for this telemetry.
     */
    public static final String REQUEST_BASE_TYPE = "Microsoft.ApplicationInsights.RequestData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Id.
     */
    private String id;

    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property StartTime.
     */
    private String startTime;

    /**
     * Backing field for property Duration.
     */
    private Long duration;

    /**
     * Backing field for property ResponseCode.
     */
    private String responseCode;

    /**
     * Backing field for property Success.
     */
    private boolean success;

    /**
     * Backing field for property HttpMethod.
     */
    private String httpMethod;

    /**
     * Backing field for property Url.
     */
    private String url;

    /**
     * Backing field for property Properties.
     */
    private HashMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private HashMap<String, Double> measurements;

    /**
     * Initializes a new instance of the <see cref="RequestData"/> class.
     */
    public RequestData()
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
     * Gets the Id property.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the Id property.
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the Name property.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the Name property.
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the StartTime property.
     */
    public String getStartTime() {
        return this.startTime;
    }

    /**
     * Sets the StartTime property.
     */
    public void setStartTime(String value) {
        this.startTime = value;
    }

    /**
     * Gets the Duration property.
     */
    public long getDuration() {
        return this.duration;
    }

    /**
     * Sets the Duration property.
     */
    public void setDuration(long value) {
        this.duration = value;
    }

    /**
     * Gets the ResponseCode property.
     */
    public String getResponseCode() {
        return this.responseCode;
    }

    /**
     * Sets the ResponseCode property.
     */
    public void setResponseCode(String value) {
        this.responseCode = value;
    }

    /**
     * Gets the Success property.
     */
    public boolean getSuccess() {
        return this.success;
    }

    /**
     * Sets the Success property.
     */
    public void setSuccess(boolean value) {
        this.success = value;
    }

    /**
     * Gets the HttpMethod property.
     */
    public String getHttpMethod() {
        return this.httpMethod;
    }

    /**
     * Sets the HttpMethod property.
     */
    public void setHttpMethod(String value) {
        this.httpMethod = value;
    }

    /**
     * Gets the Url property.
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Sets the Url property.
     */
    public void setUrl(String value) {
        this.url = value;
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

    @Override
    public String getEnvelopName() {
        return REQUEST_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return REQUEST_BASE_TYPE;
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("id", id);
        writer.write("name", name);
        writer.write("startTime", startTime);
        writer.write("duration", duration);
        writer.write("responseCode", responseCode);
        writer.write("success", success);
        writer.write("httpMethod", httpMethod);
        writer.write("url", url);
        writer.write("properties", properties);
        writer.write("measurements", measurements);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
