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
     * Initializes a new instance of the class.
     */
    public MessageData()
    {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public SeverityLevel getSeverityLevel() {
        return this.severityLevel;
    }

    public void setSeverityLevel(SeverityLevel value) {
        this.severityLevel = value;
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


    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
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

    protected void InitializeFields() {
    }
}
