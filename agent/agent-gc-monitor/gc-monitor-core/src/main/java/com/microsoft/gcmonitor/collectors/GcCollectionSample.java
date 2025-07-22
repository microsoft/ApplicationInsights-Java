// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor.collectors;

import com.microsoft.gcmonitor.GcCollectionEvent;
import com.microsoft.gcmonitor.MemoryManagement;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.management.openmbean.CompositeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of a single GCCollectionEvent formed from data from an MxBean. */
class GcCollectionSample implements GcCollectionEvent {
  private static final Logger logger = LoggerFactory.getLogger(GcCollectionSample.class);

  private static final String ID = "id";

  private static final String GC_THREAD_COUNT = "GcThreadCount";
  private static final String DURATION = "duration";
  private static final String END_TIME = "endTime";
  private static final String MEMORY_USAGE_BEFORE_GC = "memoryUsageBeforeGc";
  private static final String MEMORY_USAGE_AFTER_GC = "memoryUsageAfterGc";

  private final long id;
  private final int gcThreadCount;
  private final long duration;
  private final long endTime;
  private final GarbageCollector collector;
  private final String gcCause;
  private final String gcAction;

  private final Map<MemoryPool, MemoryUsage> memoryUsageBeforeGc;
  private final Map<MemoryPool, MemoryUsage> memoryUsageAfterGc;

  public GcCollectionSample(
      GarbageCollector collector,
      CompositeData collectionData,
      String gcCause,
      String gcAction,
      MemoryManagement memoryManagement) {
    this.collector = collector;
    id = (Long) collectionData.get(ID);
    gcThreadCount = (Integer) collectionData.get(GC_THREAD_COUNT);
    duration = (Long) collectionData.get(DURATION);
    endTime = (Long) collectionData.get(END_TIME);
    memoryUsageBeforeGc =
        groupMemoryUsageByPoolName(collectionData.get(MEMORY_USAGE_BEFORE_GC), memoryManagement);
    memoryUsageAfterGc =
        groupMemoryUsageByPoolName(collectionData.get(MEMORY_USAGE_AFTER_GC), memoryManagement);
    this.gcCause = gcCause;
    this.gcAction = gcAction;
  }

  private static Map<MemoryPool, MemoryUsage> groupMemoryUsageByPoolName(
      Object map, MemoryManagement memoryManagement) {
    @SuppressWarnings("unchecked")
    Map<List<String>, CompositeData> byName = (Map<List<String>, CompositeData>) map;
    Map<MemoryPool, MemoryUsage> byIdentifier = new HashMap<>();
    try {
      for (Map.Entry<List<String>, CompositeData> pool : byName.entrySet()) {
        Iterator<?> pair = pool.getValue().values().iterator();

        Optional<MemoryPool> name = memoryManagement.getPool((String) pair.next());
        if (name.isPresent()) {
          MemoryUsage usage = MemoryUsage.from((CompositeData) pair.next());
          byIdentifier.put(name.get(), usage);
        }
      }
    } catch (RuntimeException e) {
      logger.error("Failed to group pool data", e);
    }
    return Collections.unmodifiableMap(byIdentifier);
  }

  @Override
  public MemoryUsage getMemoryUsageBeforeGc(MemoryPool pool) {
    return memoryUsageBeforeGc.get(pool);
  }

  @Override
  public MemoryUsage getMemoryUsageBeforeGc(List<MemoryPool> pools) {
    return aggregateMemoryPools(pools, memoryUsageBeforeGc);
  }

  private static MemoryUsage aggregateMemoryPools(
      List<MemoryPool> pools, Map<MemoryPool, MemoryUsage> pool) {
    return pools.stream()
        .map(pool::get)
        .reduce(
            new MemoryUsage(0, 0, 0, -1),
            (acc, value) -> {
              long max;
              if (acc.getMax() == -1 || value.getMax() == -1) {
                max = -1;
              } else {
                max = acc.getMax() + value.getMax();
              }
              return new MemoryUsage(
                  acc.getInit() + value.getInit(),
                  acc.getUsed() + value.getUsed(),
                  acc.getCommitted() + value.getCommitted(),
                  max);
            });
  }

  @Override
  public MemoryUsage getMemoryUsageAfterGc(MemoryPool pool) {
    return memoryUsageAfterGc.get(pool);
  }

  @Override
  public MemoryUsage getMemoryUsageAfterGc(List<MemoryPool> pools) {
    return aggregateMemoryPools(pools, memoryUsageAfterGc);
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public long getEndTime() {
    return endTime;
  }

  @Override
  public long getDuration() {
    return duration;
  }

  @Override
  public int getGcThreadCount() {
    return gcThreadCount;
  }

  @Override
  public GarbageCollector getCollector() {
    return collector;
  }

  @Override
  public String getGcCause() {
    return gcCause;
  }

  @Override
  public String getGcAction() {
    return gcAction;
  }

  @Override
  public Optional<MemoryPool> getTenuredPool() {
    return memoryUsageAfterGc.keySet().stream().filter(MemoryPool::isTenuredPool).findFirst();
  }

  @Override
  public List<MemoryPool> getYoungPools() {
    return memoryUsageAfterGc.keySet().stream()
        .filter(MemoryPool::isYoungPool)
        .collect(Collectors.toList());
  }
}
