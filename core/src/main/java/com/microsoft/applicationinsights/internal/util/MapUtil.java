/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.util;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.google.common.base.Strings;

/**
 * Methods that would have been great to have on maps.
 */
public class MapUtil
{
    public static <Value> void copy(Map<String, Value> source, Map<String, Value> target) {
        if (source == null || target == null) {
            return;
        }
        for (Map.Entry<String,Value> entry : source.entrySet()) {
            String key = entry.getKey();
            if (Strings.isNullOrEmpty(key)) {
                continue;
            }

            if (!target.containsKey(key)) {
                if (target instanceof ConcurrentHashMap && entry.getValue() == null) {
                    InternalLogger.INSTANCE.trace("Skipping key '%s' while copying because of its mapping value is null", key);
                } else {
                    target.put(key, entry.getValue());
                }
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
}
