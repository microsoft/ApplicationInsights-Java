package com.microsoft.applicationinsights.util;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Strings;

/**
 * Methods that would have been great to have on maps.
 */
public class MapUtil
{
    public static <Value> void copy(Map<String, Value> source, Map<String, Value> target) {
        for (Map.Entry<String,Value> entry : source.entrySet()) {
            String key = entry.getKey();
            if (Strings.isNullOrEmpty(key)) {
                continue;
            }

            if (!target.containsKey(key)) {
                target.put(key,entry.getValue());
            }
        }
    }

    public static <Key, Value> Value getValueOrNull(Map<Key, Value> map, Key key) {
        return map.containsKey(key) ? map.get(key) : null;
    }

    public static Boolean getBoolValueOrNull(Map<String, String> map, String key) {
        return map.containsKey(key) ? Boolean.parseBoolean(map.get(key)) : null;
    }

    public static Date getDateValueOrNull(Map<String, String> map, String key) {
        try {
            return map.containsKey(key) ? LocalStringsUtils.getDateFormatter().parse(map.get(key)) : null;
        } catch (ParseException pe) {
            return null;
        }
    }

    public static void setStringValueOrRemove(Map<String, String> map, String key, String value) {
        if (Strings.isNullOrEmpty(value)) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    public static void setBoolValueOrRemove(Map<String, String> map, String key, Boolean value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value ? "true" : "false");
        }
    }

    public static void setDateValueOrRemove(Map<String, String> map, String key, Date value) {
        if (value == null)
            map.remove(key);
        else
            map.put(key, LocalStringsUtils.getDateFormatter().format(value));
    }

    public static void sanitizeProperties(Map<String, String> map) {
        if (map != null) {
            // TODO: implement this stuff.
        }
    }

    public static void sanitizeMeasurements(Map<String, Double> map) {
        if (map != null) {
            // TODO: implement this stuff.
        }
    }
}
