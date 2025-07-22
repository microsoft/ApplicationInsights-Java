// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollectors;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a group of memory managers that as a group manages the JVMs heap. Typically, for most
 * collectors this would consist of a Young generational manager and a Tenured generation manger.
 */
public enum MemoryManagers {
  PARALLEL_SCAVENGE(GarbageCollectors.PsMarkSweep.class, GarbageCollectors.PsScavenge.class),

  CONCURRENT_MARK_SWEEP(
      GarbageCollectors.ConcurrentMarkSweep.class, GarbageCollectors.ParNew.class),

  MARK_SWEEP(GarbageCollectors.MarkSweepCompact.class, GarbageCollectors.Copy.class),

  G1(GarbageCollectors.G1OldGeneration.class, GarbageCollectors.G1YoungGeneration.class),

  SHENANDOAH(GarbageCollectors.ShenandoahCycles.class, GarbageCollectors.ShenandoahPauses.class),

  ZGC(GarbageCollectors.Zgc.class);

  @SuppressWarnings("ImmutableEnumChecker")
  private final Class<? extends GarbageCollector>[] managers;

  public static MemoryManagers of(MemoryManagement manager) {
    Set<GarbageCollector> collectorIdentifiers = manager.getCollectors();
    for (MemoryManagers group : values()) {
      if (group.isComposedOf(collectorIdentifiers)) {
        return group;
      }
    }
    throw new IllegalArgumentException(
        "Unable to find garbage collector group for the memory manager");
  }

  @SafeVarargs
  MemoryManagers(Class<? extends GarbageCollector>... allCollectors) {
    this.managers = allCollectors;
  }

  private boolean isComposedOf(Set<GarbageCollector> collectors) {
    Set<Class<? extends GarbageCollector>> collectorClasses =
        collectors.stream().map(GarbageCollector::getClass).collect(Collectors.toSet());
    return collectorClasses.containsAll(Arrays.asList(managers));
  }
}
