package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.SendableData;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.MapUtil;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Superclass for all telemetry data classes.
 */
public abstract class BaseTelemetry<T extends SendableData> implements Telemetry
{
    private TelemetryContext context;
    private Date             timestamp;

    protected BaseTelemetry() {
    }

    protected void initialize(Map<String, String> properties) {
        this.context = new TelemetryContext(properties, new HashMap<String, String>());
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(Date date) {
        this.timestamp = date;
    }

    @Override
    public TelemetryContext getContext() {
        return context;
    }

    @Override
    public Map<String, String> getProperties() {
        return this.context.getProperties();
    }

    @Override
    public void sanitize() {
        MapUtil.sanitizeProperties(this.getProperties());
        additionalSanitize();
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {

        Envelope envelope = new Envelope();

        envelope.setIKey(context.getInstrumentationKey());
        envelope.setData(new Data<T>(getData()));
        envelope.setTime(LocalStringsUtils.getDateFormatter().format(getTimestamp()));
        envelope.setTags(context.getTags());

        envelope.serialize(writer);
    }

    protected abstract void additionalSanitize();

    protected abstract T getData();
}
