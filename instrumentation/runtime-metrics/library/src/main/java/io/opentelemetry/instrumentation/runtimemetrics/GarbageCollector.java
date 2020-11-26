/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.api.metrics.LongSumObserver;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Registers observers that generate metrics about JVM garbage collectors.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GarbageCollector.registerObservers();
 * }</pre>
 *
 * <p>Example metrics being exported:
 *
 * <pre>
 *   runtime.jvm.gc.collection{gc="PS1"} 6.7
 * </pre>
 */
public final class GarbageCollector {
  private static final String GC_LABEL_KEY = "gc";

  /** Register all observers provided by this module. */
  public void registerObservers() {
    List<GarbageCollectorMXBean> garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
    Meter meter = OpenTelemetry.getGlobalMeter(GarbageCollector.class.getName());
    final LongSumObserver observer =
        meter
            .longSumObserverBuilder("runtime.jvm.gc.collection")
            .setDescription("Time spent in a given JVM garbage collector in milliseconds.")
            .setUnit("ms")
            .build();
    final List<Labels> labelSets = new ArrayList<>(garbageCollectors.size());
    for (final GarbageCollectorMXBean gc : garbageCollectors) {
      labelSets.add(Labels.of(GC_LABEL_KEY, gc.getName()));
    }

    observer.setCallback(
        resultLongObserver -> {
          for (int i = 0; i < garbageCollectors.size(); i++) {
            resultLongObserver.observe(
                garbageCollectors.get(i).getCollectionTime(), labelSets.get(i));
          }
        });
  }
}
