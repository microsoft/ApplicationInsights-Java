package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

/**
 * Data contract class MessageData.
 */
public class MessageData extends Domain implements JsonSerializable {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String MESSAGE_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Message";

    /**
     * Base Type for this telemetry.
     */
    public static final String MESSAGE_BASE_TYPE = "Microsoft.ApplicationInsights.MessageData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Message.
     */
    private String message;

    /**
     * Backing field for property SeverityLevel.
     */
    private SeverityLevel severityLevel = SeverityLevel.Verbose;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Initializes a new instance of the <see cref="MessageData"/> class.
     */
    public MessageData()
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
     * Gets the Message property.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Sets the Message property.
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Gets the SeverityLevel property.
     */
    public SeverityLevel getSeverityLevel() {
        return this.severityLevel;
    }

    /**
     * Sets the SeverityLevel property.
     */
    public void setSeverityLevel(SeverityLevel value) {
        this.severityLevel = value;
    }

    /**
     * Gets the Properties property.
     */
    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentHashMap<String, String>();
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
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException
    {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("message", message);

        if (!severityLevel.equals(SeverityLevel.Verbose)) {
            writer.write("severityLevel", severityLevel.getValue());
        }

        writer.write("properties", properties);
    }

    @Override
    public String getEnvelopName() {
        return MESSAGE_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return MESSAGE_BASE_TYPE;
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {

    }
}
