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

package io.opentelemetry.instrumentation.api.instrumenter.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DurationBucketizer {

  public static final String AI_PERFORMANCE_BUCKET = "ai.performance.bucket";

  // sorted HashMap
  private static final Map<String, Double> performanceBuckets = new LinkedHashMap<>();

  static {
    performanceBuckets.put("<250ms", 250d);
    performanceBuckets.put("250ms-500ms", 500d);
    performanceBuckets.put("500ms-1sec", 1000d);
    performanceBuckets.put("1sec-3sec", 3000d);
    performanceBuckets.put("3sec-7sec", 7000d);
    performanceBuckets.put("7sec-15sec", 15000d);
    performanceBuckets.put("15sec-30sec", 30000d);
    performanceBuckets.put("30sec-1min", 60000d);
    performanceBuckets.put("1min-2min", 120000d);
    performanceBuckets.put("2min-5min", 300000d);
    performanceBuckets.put(">=5min", Double.MAX_VALUE);
  }

  public static String getPerformanceBucket(double durationInMillis) {
    for (Map.Entry<String, Double> entry : performanceBuckets.entrySet()) {
      if (durationInMillis < entry.getValue()) {
        return entry.getKey();
      }
    }
    return ">=5min";
  }

  private DurationBucketizer() {}
}
