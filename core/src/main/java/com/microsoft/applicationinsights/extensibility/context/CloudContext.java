package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.internal.util.MapUtil;

import java.util.concurrent.ConcurrentMap;

public class CloudContext {
    private final ConcurrentMap<String, String> tags;

    public CloudContext(ConcurrentMap<String, String> tags) {
        this.tags = tags;
    }

    public void setRole(String role) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getCloudRole(), role);
    }

    public String getRole() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getCloudRole());
    }

    public void setRoleInstance(String roleInstance) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getCloudRoleInstance(), roleInstance);
    }

    public String getRoleInstance() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getCloudRoleInstance());
    }
}
