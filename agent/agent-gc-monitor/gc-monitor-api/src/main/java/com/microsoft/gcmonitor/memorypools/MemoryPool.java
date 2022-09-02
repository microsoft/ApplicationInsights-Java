// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor.memorypools;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import java.util.Collections;
import java.util.Set;

/** Representation of a JVM memory pool. */
public abstract class MemoryPool {

  private final String name;
  private final Set<GarbageCollector> garbageCollectors;
  private final boolean tenuredPool;
  private final boolean youngPool;
  private final boolean heap;

  MemoryPool(
      String name,
      Set<GarbageCollector> garbageCollectors,
      boolean heap,
      boolean tenuredPool,
      boolean youngPool) {
    this.name = name;
    this.garbageCollectors = Collections.unmodifiableSet(garbageCollectors);
    this.heap = heap;
    this.tenuredPool = tenuredPool;
    this.youngPool = youngPool;
  }

  /** The name of the memory pool. */
  public String getName() {
    return name;
  }

  /** Determines if this memory pool is managed by the given collector. */
  public boolean isManagedBy(GarbageCollector collector) {
    return garbageCollectors.contains(collector);
  }

  /** If this collector manages the JVMs heap. */
  public boolean isHeap() {
    return heap;
  }

  /** If this is the JVMs tenured pool. */
  public boolean isTenuredPool() {
    return tenuredPool;
  }

  /** If this pool is part of the young generation. */
  public boolean isYoungPool() {
    return youngPool;
  }
}
