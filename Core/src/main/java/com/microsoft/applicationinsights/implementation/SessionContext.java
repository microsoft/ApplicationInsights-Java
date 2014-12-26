package com.microsoft.applicationinsights.implementation;

import com.microsoft.applicationinsights.datacontracts.*;
import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.util.MapUtil;

import java.io.IOException;
import java.util.Map;

public class SessionContext implements JsonSerializable {
    private final Map<String, String> tags;

    public SessionContext(Map<String, String> tags) {
        this.tags = tags;
    }

    String getId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getSessionId());
    }

    public void setId(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getSessionId(), version);
    }

    Boolean getIsFirst() {
        return MapUtil.getBoolValueOrNull(tags, ContextTagKeys.getKeys().getSessionIsFirst());
    }

    public void setIsFirst(Boolean version) {
        MapUtil.setBoolValueOrRemove(tags, ContextTagKeys.getKeys().getSessionIsFirst(), version);
    }

    Boolean getIsNewSession() {
        return MapUtil.getBoolValueOrNull(tags, ContextTagKeys.getKeys().getSessionIsNew());
    }

    public void setIsNewSession(Boolean version) {
        MapUtil.setBoolValueOrRemove(tags, ContextTagKeys.getKeys().getSessionIsNew(), version);
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("id", this.getId());
        writer.write("firstSession", this.getIsFirst());
        writer.write("isNewSession", this.getIsNewSession());
    }
}