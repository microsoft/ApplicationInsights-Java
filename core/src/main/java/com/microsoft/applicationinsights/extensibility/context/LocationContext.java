package com.microsoft.applicationinsights.extensibility.context;

import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.internal.util.MapUtil;

import com.google.common.base.Strings;

public final class LocationContext {
    private static final String PATTERN =
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private final ConcurrentMap<String, String> tags;

    public LocationContext(ConcurrentMap<String, String> tags) {
        this.tags = tags;
    }

    String getIp() {
        return MapUtil.getValueOrNull(tags, ContextTagKeys.getKeys().getLocationIP());
    }

    public void setIp(String value) {
        if (!Strings.isNullOrEmpty(value) && isIPV4(value)) {
            MapUtil.setStringValueOrRemove(tags, ContextTagKeys.getKeys().getLocationIP(), value);
        }
    }

    private boolean isIPV4(String ip) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}