package com.microsoft.applicationinsights.implementation;

import com.microsoft.applicationinsights.datacontracts.*;
import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.util.MapUtil;
import com.microsoft.applicationinsights.util.StringUtil;

import java.io.IOException;
import java.util.Map;

public class LocationContext implements JsonSerializable
{
    private final Map<String, String> tags;

    public LocationContext(Map<String, String> tags)
    {
        this.tags = tags;
    }

    String getIp()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getLocationIP());
    }

    public void setIp(String value)
    {
        if (!StringUtil.isNullOrEmpty(value) && isIPV4(value))
        {
            MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getLocationIP(), value);
        }
    }

    @Override
    public void serialize(com.microsoft.applicationinsights.datacontracts.JsonWriter writer) throws IOException
    {
        writer.writeStartObject();
        writer.writeProperty("ip", this.getIp());
        writer.writeEndObject();
    }

    private boolean isIPV4(String ip)
    {
        if ((ip.length() > 15) || ip.length() < 7)
            return false;

        for (char c : ip.toCharArray())
        {
            if (c >= '0' && c <= '9')
                continue;
            if (c != '.')
                return false;
        }

        String[] strArray = ip.split(".");
        if (strArray.length != 4)
            return false;

        for (String str : strArray)
        {
            if (!StringUtil.tryParseByte(str))
                return false;
        }

        return true;
    }
}