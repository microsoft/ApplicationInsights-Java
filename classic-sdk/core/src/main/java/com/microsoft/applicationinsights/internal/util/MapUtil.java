// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.util;

import java.util.Date;
import java.util.Map;

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
      if (!target.containsKey(key) && entry.getValue() != null) {
        target.put(key, entry.getValue());
      }
    }
  }

  public static <K, V> V getValueOrNull(Map<K, V> map, K key) {
    return map.get(key);
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
