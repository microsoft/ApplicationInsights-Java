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
     * Initializes a new instance of the class.
     */
    public PageViewData()
    {
        this.InitializeFields();
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public Long getDuration() {
        return this.duration;
    }

    public void setDuration(Long value) {
        this.duration = value;
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("url", url);
        writer.write("duration", duration);
    }

    protected void InitializeFields() {
    }
}
