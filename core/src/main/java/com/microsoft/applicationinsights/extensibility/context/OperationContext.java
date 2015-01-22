package com.microsoft.applicationinsights.extensibility.context;

import java.util.Map;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class OperationContext {
    private final Map<String, String> tags;

    public OperationContext(Map<String, String> tags) {
        this.tags = tags;
    }

    String getId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getOperationId());
    }

    public void setId(String id) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getOperationId(), id);
    }

    String getName() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getOperationName());
    }

    public void setName(String name) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getOperationName(), name);
    }
}