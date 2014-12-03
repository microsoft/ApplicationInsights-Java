package com.microsoft.applicationinsights.implementation;

import com.microsoft.applicationinsights.datacontracts.*;
import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.util.MapUtil;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class UserContext implements JsonSerializable
{
    private final Map<String,String> tags;

    public UserContext(Map<String, String> tags)
    {
        this.tags = tags;
    }

    String getId()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserId());
    }

    public void setId(String version)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserId(), version);
    }

    String getAccountId()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserAccountId());
    }

    public void setAccountId(String version)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountId(), version);
    }

    String getUserAgent()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getUserAgent());
    }

    public void setUserAgent(String version)
    {
        MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getUserAgent(), version);
    }

    Date getAcquisitionDate()
    {
        return MapUtil.getDateValueOrNull(tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate());
    }

    public void setgetAcquisitionDate(Date version)
    {
        MapUtil.setDateValueOrRemove(tags, ContextTagKeys.getKeys().getUserAccountAcquisitionDate(), version);
    }

    @Override
    public void serialize(com.microsoft.applicationinsights.datacontracts.JsonWriter writer) throws IOException
    {
        writer.writeStartObject();
        writer.writeProperty("id", this.getId());
        writer.writeProperty("userAgent", this.getUserAgent());
        writer.writeProperty("accountId", this.getAccountId());
        writer.writeProperty("anonUserAcquisitionDate", this.getAcquisitionDate());
        writer.writeEndObject();
    }
}