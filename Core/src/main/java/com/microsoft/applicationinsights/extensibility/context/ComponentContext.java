package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.internal.util.MapUtil;

import java.io.IOException;
import java.util.Map;

public class ComponentContext implements JsonSerializable {
    private final Map<String, String> tags;

    public ComponentContext(Map<String, String> tags)
    {
        this.tags = tags;
    }

    String getVersion()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getApplicationVersion());
    }

    public void setVersion(String version)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getApplicationVersion(), version);
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("version", this.getVersion());
    }
}