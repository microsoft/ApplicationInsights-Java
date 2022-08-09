package com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics;

import java.util.HashMap;
import java.util.Map;

public class DurationBucketizer {

  private static final Map<String, Double> performanceBuckets = new HashMap<>();

  static {
    performanceBuckets.put("<250ms", 250.0);
    performanceBuckets.put("250ms-500ms", 500.0);
    performanceBuckets.put("500ms-1sec", 1000.0);
    performanceBuckets.put("1sec-3sec", 3000.0);
    performanceBuckets.put("3sec-7sec", 7000.0);
    performanceBuckets.put("7sec-15sec", 15000.0);
    performanceBuckets.put("15sec-30sec", 30000.0);
    performanceBuckets.put("30sec-1min", 60000.0);
    performanceBuckets.put("1min-2min", 120000.0);
    performanceBuckets.put("2min-5min", 300000.0);
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
}
