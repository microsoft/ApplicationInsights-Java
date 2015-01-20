package com.microsoft.applicationinsights.extensibility.context;

import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class ComponentContext {
    private final ConcurrentMap<String, String> tags;

    public ComponentContext(ConcurrentMap<String, String> tags) {
        this.tags = tags;
    }

    String getVersion() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getApplicationVersion());
    }

    public void setVersion(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getApplicationVersion(), version);
    }
}