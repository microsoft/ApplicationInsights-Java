// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Optional;

/** A garbage collection event reported by a gc mxbean. */
public interface GcCollectionEvent {
  /** Returns the memory usage before the collection for the given memory pool. */
  MemoryUsage getMemoryUsageBeforeGc(MemoryPool pools);

  /** Returns the aggregate memory usage before the collection for the given memory pools. */
  MemoryUsage getMemoryUsageBeforeGc(List<MemoryPool> pools);

  /** Returns the memory usage after the collection for the given memory pool. */
  MemoryUsage getMemoryUsageAfterGc(MemoryPool pool);

  /** Returns the aggregate memory usage after the collection for the given memory pools. */
  MemoryUsage getMemoryUsageAfterGc(List<MemoryPool> pools);

  /** The garbage collection id. */
  long getId();

  /** The end time of the collection in ms since the JVM started. */
  long getEndTime();

  /** The duration of the collection in ms. */
  long getDuration();

  /** Thread count that the collection ran with. */
  int getGcThreadCount();

  /** The type of collector that produced this event. */
  GarbageCollector getCollector();

  /** The cause of the collection. */
  String getGcCause();

  /** The action performed by the GC. */
  String getGcAction();

  /** Returns the tenured pool of this JVM. */
  Optional<MemoryPool> getTenuredPool();

  /** Returns the young pools of this JVM. */
  List<MemoryPool> getYoungPools();
}
