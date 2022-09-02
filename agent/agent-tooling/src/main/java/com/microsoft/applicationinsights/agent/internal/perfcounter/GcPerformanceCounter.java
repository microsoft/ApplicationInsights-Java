// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/** The class reports GC related data. */
public final class GcPerformanceCounter implements PerformanceCounter {

  private static final String GC_TOTAL_COUNT = "GC Total Count";
  private static final String GC_TOTAL_TIME = "GC Total Time";

  private long currentTotalCount = 0;
  private long currentTotalTime = 0;

  @Override
  public void report(TelemetryClient telemetryClient) {
    synchronized (this) {
      List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
      if (gcs.isEmpty()) {
        return;
      }

      long totalCollectionCount = 0;
      long totalCollectionTime = 0;
      for (GarbageCollectorMXBean gc : gcs) {
        long gcCollectionCount = gc.getCollectionCount();
        if (gcCollectionCount > 0) {
          totalCollectionCount += gcCollectionCount;
        }

        long gcCollectionTime = gc.getCollectionTime();
        if (gcCollectionTime > 0) {
          totalCollectionTime += gcCollectionTime;
        }
      }

      long countToReport = totalCollectionCount - currentTotalCount;
      long timeToReport = totalCollectionTime - currentTotalTime;

      currentTotalCount = totalCollectionCount;
      currentTotalTime = totalCollectionTime;

      telemetryClient.trackAsync(
          telemetryClient.newMetricTelemetry(GC_TOTAL_COUNT, (double) countToReport));
      telemetryClient.trackAsync(
          telemetryClient.newMetricTelemetry(GC_TOTAL_TIME, (double) timeToReport));
    }
  }
}
