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

package com.microsoft.gcmonitor;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Optional;

/** A garbage collection event reported by a gc mxbean */
public interface GCCollectionEvent {
  /** Returns the memory usage before the collection for the given memory pool */
  MemoryUsage getMemoryUsageBeforeGc(MemoryPool pools);

  /** Returns the aggregate memory usage before the collection for the given memory pools */
  MemoryUsage getMemoryUsageBeforeGc(List<MemoryPool> pools);

  /** Returns the memory usage after the collection for the given memory pool */
  MemoryUsage getMemoryUsageAfterGc(MemoryPool pool);

  /** Returns the aggregate memory usage after the collection for the given memory pools */
  MemoryUsage getMemoryUsageAfterGc(List<MemoryPool> pools);

  /** The garbage collection id */
  long getId();

  /** The end time of the collection in ms since the JVM started */
  long getEndTime();

  /** The duration of the collection in ms */
  long getDuration();

  /** Thread count that the collection ran with */
  int getGcThreadCount();

  /** The type of collector that produced this event */
  GarbageCollector getCollector();

  /** The cause of the collection */
  String getGcCause();

  /** The action performed by the GC */
  String getGcAction();

  /** Returns the tenured pool of this JVM */
  Optional<MemoryPool> getTenuredPool();

  /** Returns the young pools of this JVM */
  List<MemoryPool> getYoungPools();
}
