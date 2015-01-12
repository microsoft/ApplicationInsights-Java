package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

/**
 * Data contract class PageViewData.
 */
public class PageViewData extends EventData {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EnvelopeName = "Microsoft.ApplicationInsights.PageView";

    /**
     * Base Type for this telemetry.
     */
    public static final String BaseType = "Microsoft.ApplicationInsights.PageViewData";

    /**
     * Backing field for property Url.
     */
    private String url;

    /**
     * Backing field for property Duration.
     */
    private long duration;

    /**
     * Initializes a new instance of the <see cref="PageViewData"/> class.
     */
    public PageViewData()
    {
        this.InitializeFields();
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
     * Gets the Duration property.
     */
    public Long getDuration() {
        return this.duration;
    }

    /**
     * Sets the Duration property.
     */
    public void setDuration(Long value) {
        this.duration = value;
    }


    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("url", url);
        writer.write("duration", duration);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
