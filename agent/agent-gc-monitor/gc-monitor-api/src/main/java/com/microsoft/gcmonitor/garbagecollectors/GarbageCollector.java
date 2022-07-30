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
