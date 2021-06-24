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
