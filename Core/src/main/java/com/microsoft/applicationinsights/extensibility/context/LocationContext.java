package com.microsoft.applicationinsights.extensibility.context;

import java.io.IOException;
import java.util.Map;

import com.microsoft.applicationinsights.extensibility.model.ContextTagKeys;
import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.MapUtil;

import com.google.common.base.Strings;

public class LocationContext implements JsonSerializable {
    private final Map<String, String> tags;

    public LocationContext(Map<String, String> tags)
    {
        this.tags = tags;
    }

    String getIp()
    {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getLocationIP());
    }

    public void setIp(String value) {
        if (!Strings.isNullOrEmpty(value) && isIPV4(value)) {
            MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getLocationIP(), value);
        }
    }

    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("ip", this.getIp());
    }

    private boolean isIPV4(String ip) {
        if ((ip.length() > 15) || ip.length() < 7) {
            return false;
        }

        for (char c : ip.toCharArray()) {
            if (c >= '0' && c <= '9')
                continue;
            if (c != '.')
                return false;
        }

        String[] strArray = ip.split(".");
        if (strArray.length != 4) {
            return false;
        }

        for (String str : strArray) {
            if (!LocalStringsUtils.tryParseByte(str)) {
                return false;
            }
        }

        return true;
    }
}