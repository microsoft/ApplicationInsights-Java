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

import com.microsoft.applicationinsights.common.Strings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Methods that would have been great to have on maps. */
public class MapUtil {
  /**
   * Copies entries from the source map to the target map, overwrites any values in target. Filters
   * out null values if target is a {@link ConcurrentHashMap}.
   *
   * @param source the source map. If null or empty, this is a nop.
   * @param target the target map. Cannot be null.
   * @param <V> The type of the values in both maps
   * @throws IllegalArgumentException if either {@code source} or {@code target} are null.
   */
  public static <V> void copy(Map<String, V> source, Map<String, V> target) {
    if (target == null) {
      throw new IllegalArgumentException("target must not be null");
    }

    if (source == null || source.isEmpty()) {
      return;
    }

    for (Map.Entry<String, V> entry : source.entrySet()) {
      String key = entry.getKey();
      if (Strings.isNullOrEmpty(key)) {
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

  private MapUtil() {}
}
