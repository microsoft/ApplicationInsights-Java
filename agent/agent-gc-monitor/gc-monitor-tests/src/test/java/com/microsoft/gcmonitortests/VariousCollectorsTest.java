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

package com.microsoft.gcmonitortests;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.gcmonitor.GCCollectionEvent;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class VariousCollectorsTest {

  @Test
  void testCms() throws IOException, UnableToMonitorMemoryException, InterruptedException {
    try {
      List<GCCollectionEvent> events =
          new GcProcessRunner("-XX:+UseConcMarkSweepGC", 50).getGcCollectionEvents();

      assetGcsArePresent(events);
    } catch (GCNotPresentException e) {
      // CMS deprecated
    }
  }

  private static void assetGcsArePresent(List<GCCollectionEvent> events) {
    assertThat(youngGcIsPresent(events)).isTrue();
    assertThat(tenuredGcIsPresent(events)).isTrue();
    assertThat(systemGcIsPresent(events)).isTrue();
  }

  @Test
  void testParallel()
      throws IOException, UnableToMonitorMemoryException, InterruptedException,
          GCNotPresentException {
    List<GCCollectionEvent> events =
        new GcProcessRunner("-XX:+UseParallelGC", 70).getGcCollectionEvents();

    print(events);

    assetGcsArePresent(events);
  }

  @Test
  void testG1()
      throws IOException, UnableToMonitorMemoryException, InterruptedException,
          GCNotPresentException {
    List<GCCollectionEvent> events =
        new GcProcessRunner("-XX:+UseG1GC", 50).getGcCollectionEvents();

    assetGcsArePresent(events);
  }

  @Test
  void testSerial() throws IOException, UnableToMonitorMemoryException, InterruptedException {
    try {
      List<GCCollectionEvent> events =
          new GcProcessRunner("-XX:+UseSerialGC", 50).getGcCollectionEvents();
      assetGcsArePresent(events);
    } catch (GCNotPresentException e) {
      // to be expected for some time
    }
  }

  // TODO: Enable when this can use Java 11
  @Disabled
  @Test
  void testShenandoah() throws IOException, UnableToMonitorMemoryException, InterruptedException {
    try {
      List<GCCollectionEvent> events =
          new GcProcessRunner("-XX:+UseShenandoahGC", 50).getGcCollectionEvents();

      assetGcsArePresent(events);
    } catch (GCNotPresentException e) {
      // to be expected for some time
    }
  }

  // TODO: Enable when this can use Java 11
  @Disabled
  @Test
  void testZ() throws IOException, UnableToMonitorMemoryException, InterruptedException {
    try {
      List<GCCollectionEvent> events =
          new GcProcessRunner("-XX:+UseZGC", 200).getGcCollectionEvents();
      assetGcsArePresent(events);
    } catch (GCNotPresentException e) {
      // to be expected for some time
    }
  }

  private static boolean tenuredGcIsPresent(List<GCCollectionEvent> events) {
    return isPresent(
        events, event -> event.getCollector().isTenuredCollector() && memoryValuesAreSane(event));
  }

  private static boolean youngGcIsPresent(List<GCCollectionEvent> events) {
    return isPresent(
        events, event -> event.getCollector().isYoungCollector() && memoryValuesAreSane(event));
  }

  private static boolean memoryValuesAreSane(GCCollectionEvent event) {
    return event.getMemoryUsageBeforeGc(event.getTenuredPool().get()).getUsed() > 0
        && event.getMemoryUsageBeforeGc(event.getYoungPools()).getUsed() > 0
        && event.getMemoryUsageAfterGc(event.getTenuredPool().get()).getUsed() > 0;
  }

  private static boolean systemGcIsPresent(List<GCCollectionEvent> events) {
    return isPresent(
        events, event -> event.getGcCause().contains("System.gc()") && memoryValuesAreSane(event));
  }

  private static boolean isPresent(
      List<GCCollectionEvent> events, Predicate<GCCollectionEvent> predicate) {
    return events.stream().anyMatch(predicate);
  }

  @SuppressWarnings("SystemOut")
  private static void print(List<GCCollectionEvent> events) {
    System.out.println("Obtained: " + events.size());
    events.forEach(
        event -> {
          Optional<MemoryPool> tenuredPool = event.getTenuredPool();
          if (tenuredPool.isPresent()) {
            System.out.println(
                new StringJoiner(",")
                    .add(Long.toString(event.getId()))
                    .add(event.getGcCause())
                    .add(Long.toString(event.getDuration()))
                    .add(Long.toString(event.getEndTime()))
                    .add(event.getCollector().toString())
                    .add(event.getGcAction())
                    .add(Long.toString(event.getMemoryUsageBeforeGc(tenuredPool.get()).getUsed()))
                    .add(Long.toString(event.getMemoryUsageAfterGc(tenuredPool.get()).getUsed())));
          }
        });
  }
}
