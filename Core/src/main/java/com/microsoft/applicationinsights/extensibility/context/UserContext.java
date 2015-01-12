package com.microsoft.applicationinsights.extensibility.context;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.internal.util.MapUtil;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class UserContext implements JsonSerializable {
    private final Map<String,String> tags;

    public UserContext(Map<String, String> tags)
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

    public void setgetAcquisitionDate(Date version) {
        MapUtil.setDateValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate(), version);
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("id", this.getId());
        writer.write("userAgent", this.getUserAgent());
        writer.write("accountId", this.getAccountId());
        writer.write("anonUserAcquisitionDate", this.getAcquisitionDate());
    }
}