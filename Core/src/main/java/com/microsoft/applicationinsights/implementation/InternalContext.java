package com.microsoft.applicationinsights.implementation;

import com.microsoft.applicationinsights.datacontracts.*;
import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.util.MapUtil;

import java.io.IOException;
import java.util.Map;

public class InternalContext implements JsonSerializable {
    private final Map<String, String> tags;

    public InternalContext(Map<String, String> tags)
    {
        this.tags = tags;
    }

    String getSdkVersion()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getInternalSdkVersion());
    }

    public void setSdkVersion(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getInternalSdkVersion(), version);
    }

    String getAgentVersion() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getInternalAgentVersion());
    }

    public void setAgentVersion(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getInternalAgentVersion(), version);
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("sdkVersion", this.getSdkVersion());
        writer.write("agentVersion", this.getAgentVersion());
    }
}