package com.microsoft.applicationinsights.datacontracts;

import com.microsoft.applicationinsights.extensibility.model.MessageData;
import com.microsoft.applicationinsights.util.MapUtil;
import com.microsoft.applicationinsights.util.StringUtil;

import java.io.IOException;

/**
 * Telemetry used to track events.
 */
public class TraceTelemetry extends BaseTelemetry
{
    private final MessageData data;

    public TraceTelemetry()
    {
        super();
        this.data = new MessageData();
        initialize(this.data.getProperties());
    }

    public TraceTelemetry(String message)
    {
        this();
        this.setMessage(message);
    }

    public String getMessage()
    {
        return this.data.getMessage();
    }

    public void setMessage(String message)
    {
        this.data.setMessage(message);
    }

    @Override
    public void sanitize()
    {
        this.data.setMessage(StringUtil.sanitize(this.data.getMessage(), 32768));
        MapUtil.sanitizeProperties(this.getProperties());
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException
    {
        writer.writeStartObject();

        writer.writeProperty("ver", 1);
        writer.writeProperty("name", "Microsoft.ApplicationInsights.Message");
        writer.writeProperty("time", this.getTimestamp());

        getContext().serialize(writer);

        writer.writePropertyName("data");

        {
            writer.writeStartObject();

            writer.writeProperty("type", "Microsoft.ApplicationInsights.MessageData");

            writer.writePropertyName("item");
            {
                writer.writeStartObject();
                writer.writeProperty("ver", this.data.getVer());
                writer.writeProperty("message", StringUtil.populateRequiredStringWithNullValue(this.data.getMessage(), "message", TraceTelemetry.class.getName()));
                writer.writeProperty("properties", this.data.getProperties());
                writer.writeEndObject();
            }

            writer.writeEndObject();
        }

        writer.writeEndObject();
    }
}
