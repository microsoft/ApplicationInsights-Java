package com.microsoft.applicationinsights.datacontracts;

import com.microsoft.applicationinsights.extensibility.model.EventData;
import com.microsoft.applicationinsights.util.MapUtil;
import com.microsoft.applicationinsights.util.StringUtil;

import java.io.IOException;
import java.util.Map;

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
        if (StringUtil.isNullOrEmpty(name))
            throw new IllegalArgumentException("The event name cannot be null or empty");
        this.data.setName(name);
    }

    @Override
    public void sanitize()
    {
        this.data.setName(StringUtil.sanitize(this.data.getName(), 1024));
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
                writer.writeProperty("name", StringUtil.populateRequiredStringWithNullValue(this.data.getName(), "name", EventTelemetry.class.getName()));
                writer.writeMetricsProperty("measurements", this.data.getMeasurements());
                writer.writeProperty("properties", this.data.getProperties());
                writer.writeEndObject();
            }

            writer.writeEndObject();
        }

        writer.writeEndObject();
    }
}
