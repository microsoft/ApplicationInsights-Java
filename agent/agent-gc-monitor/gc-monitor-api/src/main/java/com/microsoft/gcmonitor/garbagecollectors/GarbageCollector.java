// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor.garbagecollectors;

/** Represents a garbage collector that manages some area of memory. */
public class GarbageCollector implements GarbageCollectorStats {

  private final boolean managesHeap;
  private final String name;
  private final boolean tenuredCollector;

  // Proxy to the underlying collector that provides GC data
  private final GarbageCollectorStats proxy;
  private final boolean youngCollector;

  public GarbageCollector(
      GarbageCollectorStats proxy,
      String name,
      boolean managesHeap,
      boolean tenuredCollector,
      boolean youngCollector) {
    this.proxy = proxy;
    this.managesHeap = managesHeap;
    this.name = name;
    this.tenuredCollector = tenuredCollector;
    this.youngCollector = youngCollector;
  }

  /** The number of collections that have occurred for this collector. */
  @Override
  public long getCollectionCount() {
    return proxy.getCollectionCount();
  }

  /** The cumulative amount of time this collector has spent executing. */
  @Override
  public long getCollectionTime() {
    return proxy.getCollectionTime();
  }

  /** If this collector manages part of the Java heap. I.e CodeCacheManager does not */
  public boolean managesHeap() {
    return managesHeap;
  }

  /** The collector name. */
  public String getName() {
    return name;
  }

  /** If this collector manages the tenured generation. */
  public boolean isTenuredCollector() {
    return tenuredCollector;
  }

  /** If this collector manages the young generation. */
  public boolean isYoungCollector() {
    return youngCollector;
  }
}
