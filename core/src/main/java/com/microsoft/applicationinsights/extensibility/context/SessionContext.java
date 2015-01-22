package com.microsoft.applicationinsights.extensibility.context;

import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class SessionContext {
    private final ConcurrentMap<String, String> tags;

    public SessionContext(ConcurrentMap<String, String> tags) {
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
}