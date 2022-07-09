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
import static org.junit.jupiter.api.condition.JRE.JAVA_11;
import static org.junit.jupiter.api.condition.JRE.JAVA_13;
import static org.junit.jupiter.api.condition.OS.LINUX;

import com.microsoft.gcmonitor.GcCollectionEvent;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;

class VariousCollectorsTest {

  @Test
  @EnabledForJreRange(max = JAVA_13)
  void testCms() throws Exception {
    testGc("-XX:+UseConcMarkSweepGC", 50);
  }

  @Test
  void testParallel() throws Exception {
    testGc("-XX:+UseParallelGC", 70);
  }

  @Test
  void testG1() throws Exception {
    testGc("-XX:+UseG1GC", 50);
  }

  @Test
  void testSerial() throws Exception {
    testGc("-XX:+UseSerialGC", 50);
  }

  @Test
  @EnabledForJreRange(min = JAVA_11)
  void testShenandoah() throws Exception {
    testGc("-XX:+UseShenandoahGC", 50);
  }

  @Test
  @EnabledForJreRange(min = JAVA_11)
  @EnabledOnOs(LINUX)
  void testZ() throws Exception {
    testGc("-XX:+UseZGC", 200);
  }

  private static void testGc(String gcArg, int heapSizeInMb) throws Exception {
    List<GcCollectionEvent> events =
        new GcProcessRunner(gcArg, heapSizeInMb).getGcCollectionEvents();

    assetGcsArePresent(events);
  }

  private static void assetGcsArePresent(List<GcCollectionEvent> events) {
    assertThat(youngGcIsPresent(events)).isTrue();
    assertThat(tenuredGcIsPresent(events)).isTrue();
    assertThat(systemGcIsPresent(events)).isTrue();
  }

  private static boolean tenuredGcIsPresent(List<GcCollectionEvent> events) {
    return isPresent(
        events, event -> event.getCollector().isTenuredCollector() && memoryValuesAreSane(event));
  }

  private static boolean youngGcIsPresent(List<GcCollectionEvent> events) {
    return isPresent(
        events, event -> event.getCollector().isYoungCollector() && memoryValuesAreSane(event));
  }

  private static boolean memoryValuesAreSane(GcCollectionEvent event) {
    return event.getMemoryUsageBeforeGc(event.getTenuredPool().get()).getUsed() > 0
        && event.getMemoryUsageBeforeGc(event.getYoungPools()).getUsed() > 0
        && event.getMemoryUsageAfterGc(event.getTenuredPool().get()).getUsed() > 0;
  }

  private static boolean systemGcIsPresent(List<GcCollectionEvent> events) {
    return isPresent(
        events, event -> event.getGcCause().contains("System.gc()") && memoryValuesAreSane(event));
  }

  private static boolean isPresent(
      List<GcCollectionEvent> events, Predicate<GcCollectionEvent> predicate) {
    return events.stream().anyMatch(predicate);
  }

  @SuppressWarnings({"SystemOut", "unused"})
  private static void print(List<GcCollectionEvent> events) {
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
