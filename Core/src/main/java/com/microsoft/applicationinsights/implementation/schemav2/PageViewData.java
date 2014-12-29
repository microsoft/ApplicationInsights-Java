package com.microsoft.applicationinsights.implementation.schemav2;

import java.io.IOException;

import com.microsoft.applicationinsights.datacontracts.JsonTelemetryDataSerializer;

/**
 * Data contract class PageViewData.
 */
public class PageViewData extends EventData {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String PAGE_VIEW_ENVELOPE_NAME = "Microsoft.ApplicationInsights.PageView";

    /**
     * Base Type for this telemetry.
     */
    public static final String PAGE_VIEW_BASE_TYPE = "Microsoft.ApplicationInsights.PageViewData";

    /**
     * Backing field for property Url.
     */
    private String url;

    /**
     * Backing field for property Duration.
     */
    private String duration;

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
    public String getDuration() {
        return this.duration;
    }

    /**
     * Sets the Duration property.
     */
    public void setDuration(String value) {
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

    @Override
    public String getEnvelopName() {
        return PAGE_VIEW_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return PAGE_VIEW_BASE_TYPE;
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }
}
