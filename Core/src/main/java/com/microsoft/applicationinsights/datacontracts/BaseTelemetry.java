package com.microsoft.applicationinsights.datacontracts;

import com.microsoft.applicationinsights.channel.Telemetry;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Superclass for all telemetry data classes.
 */
public abstract class BaseTelemetry implements Telemetry
{
    private TelemetryContext context;
    private   Date             timestamp;

    protected BaseTelemetry()
    {
    }

    protected void initialize(Map<String, String> properties)
    {
        this.context = new TelemetryContext(properties, new HashMap<String, String>());
    }

    @Override
    public Date getTimestamp()
    {
        return timestamp;
    }

    @Override
    public void setTimestamp(Date date)
    {
        this.timestamp = date;
    }

    @Override
    public TelemetryContext getContext()
    {
        return context;
    }

    @Override
    public Map<String, String> getProperties()
    {
        return this.context.getProperties();
    }

    @Override
    public abstract void sanitize();

    @Override
    public abstract void serialize(JsonWriter writer) throws IOException;
}
