package com.microsoft.applicationinsights.extensibility.context;

import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class InternalContext {
    private final ConcurrentMap<String, String> tags;

    public InternalContext(ConcurrentMap<String, String> tags) {
        this.tags = tags;
    }

    String getSdkVersion() {
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
}