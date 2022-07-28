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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public class MapUtil {

  public static <V> void copy(Map<String, V> source, Map<String, V> target) {
    if (target == null) {
      throw new IllegalArgumentException("target must not be null");
    }

    if (source == null || source.isEmpty()) {
      return;
    }

    for (Map.Entry<String, V> entry : source.entrySet()) {
      String key = entry.getKey();
      if (LocalStringsUtils.isNullOrEmpty(key)) {
        continue;
      }

      if (!target.containsKey(key)) {
        if (target instanceof ConcurrentHashMap && entry.getValue() == null) {
          continue;
        } else {
          target.put(key, entry.getValue());
        }
      }
    }
  }

  public static <K, V> V getValueOrNull(Map<K, V> map, K key) {
    return map.containsKey(key) ? map.get(key) : null;
  }

  @Nullable
  public static Boolean getBoolValueOrNull(Map<String, String> map, String key) {
    return map.containsKey(key) ? Boolean.valueOf(map.get(key)) : null;
  }

  public static void setStringValueOrRemove(Map<String, String> map, String key, String value) {
    if (LocalStringsUtils.isNullOrEmpty(value)) {
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
    if (value == null) {
      map.remove(key);
    } else {
      map.put(key, LocalStringsUtils.formatDate(value));
    }
  }

  private MapUtil() {}
}
