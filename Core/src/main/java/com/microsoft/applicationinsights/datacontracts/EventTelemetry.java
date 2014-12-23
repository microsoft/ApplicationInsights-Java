package com.microsoft.applicationinsights.datacontracts;

import java.io.IOException;
import java.util.Map;

import com.microsoft.applicationinsights.extensibility.model.EventData;
import com.microsoft.applicationinsights.util.LocalStringsUtils;
import com.microsoft.applicationinsights.util.MapUtil;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public class EventTelemetry extends BaseTelemetry
{
    private final EventData data;

    public EventTelemetry()
    {
        super();
        this.data = new EventData();
        initialize(this.data.getProperties());
    }

    public EventTelemetry(String name)
    {
        this();
        this.setName(name);
    }

    public Map<String,Double> getMetrics()
    {
        return this.data.getMeasurements();
    }

    public String getName()
    {
        return this.data.getName();
    }

    public void setName(String name)
    {
        if (Strings.isNullOrEmpty(name))
            throw new IllegalArgumentException("The event name cannot be null or empty");
        this.data.setName(name);
    }

    @Override
    public void sanitize()
    {
        this.data.setName(LocalStringsUtils.sanitize(this.data.getName(), 1024));
        MapUtil.sanitizeProperties(this.getProperties());
        MapUtil.sanitizeMeasurements(this.getMetrics());
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException
    {
        writer.writeStartObject();

        writer.writeProperty("ver", 1);
        writer.writeProperty("name", "Microsoft.ApplicationInsights.Event");
        writer.writeProperty("time", this.getTimestamp());

        getContext().serialize(writer);

        writer.writePropertyName("data");

        {
            writer.writeStartObject();

            writer.writeProperty("type", "Microsoft.ApplicationInsights.EventData");

            writer.writePropertyName("item");
            {
                writer.writeStartObject();
                writer.writeProperty("ver", this.data.getVer());
                writer.writeProperty("name", LocalStringsUtils.populateRequiredStringWithNullValue(this.data.getName(), "name", EventTelemetry.class.getName()));
                writer.writeMetricsProperty("measurements", this.data.getMeasurements());
                writer.writeProperty("properties", this.data.getProperties());
                writer.writeEndObject();
            }

            writer.writeEndObject();
        }

        writer.writeEndObject();
    }
}
