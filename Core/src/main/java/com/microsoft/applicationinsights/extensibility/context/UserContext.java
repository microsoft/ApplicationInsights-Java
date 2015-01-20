package com.microsoft.applicationinsights.extensibility.context;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.util.MapUtil;

public final class UserContext {
    private final ConcurrentMap<String,String> tags;

    public UserContext(ConcurrentMap<String, String> tags)
    {
        this.tags = tags;
    }

    String getId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserId());
    }

    public void setId(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserId(), version);
    }

    String getAccountId() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserAccountId());
    }

    public void setAccountId(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountId(), version);
    }

    String getUserAgent() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserAgent());
    }

    public void setUserAgent(String version) {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAgent(), version);
    }

    Date getAcquisitionDate() {
        return MapUtil.getDateValueOrNull(tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate());
    }

    public void setAcquisitionDate(Date version) {
        MapUtil.setDateValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate(), version);
    }
}