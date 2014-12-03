package com.microsoft.applicationinsights.implementation;

import com.microsoft.applicationinsights.datacontracts.*;
import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.util.MapUtil;

import java.io.IOException;
import java.util.Map;

public class ComponentContext implements JsonSerializable
{
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

    //@Override
    public void serialize(com.microsoft.applicationinsights.datacontracts.JsonWriter writer) throws IOException
    {
        writer.writeStartObject();
        writer.writeProperty("version", this.getVersion());
        writer.writeEndObject();
    }
}