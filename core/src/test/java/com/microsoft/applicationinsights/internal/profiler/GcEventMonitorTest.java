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

package com.microsoft.applicationinsights.internal.profiler;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.profiler.config.AlertConfigParser;
import com.microsoft.gcmonitor.GcCollectionEvent;
import com.microsoft.gcmonitor.GcEventConsumer;
import com.microsoft.gcmonitor.GcMonitorFactory;
import com.microsoft.gcmonitor.MemoryManagement;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.MemoryUsage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.management.MBeanServerConnection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GcEventMonitorTest {

  @Test
  void endToEndAlertIsTriggered()
      throws ExecutionException, InterruptedException, TimeoutException {

    CompletableFuture<AlertBreach> alertFuture = new CompletableFuture<>();
    AlertingSubsystem alertingSubsystem = getAlertingSubsystem(alertFuture);

    GcMonitorFactory factory =
        new GcMonitorFactory() {
          @Override
          public MemoryManagement monitorSelf(
              ExecutorService executorService, GcEventConsumer consumer) {
            consumer.accept(mockGcEvent());
            return null;
          }

          @Override
          public MemoryManagement monitor(
              MBeanServerConnection connection,
              ExecutorService executorService,
              GcEventConsumer consumer) {
            return null;
          }
        };

    GcEventMonitor.init(
        alertingSubsystem,
        new TelemetryClient(),
        Executors.newSingleThreadExecutor(),
        new GcEventMonitor.GcEventMonitorConfiguration(GcReportingLevel.NONE),
        factory);

    AlertBreach alert = alertFuture.get(10, TimeUnit.SECONDS);

    assertThat(alert.getAlertValue()).isEqualTo(90.0);
  }

  private static AlertingSubsystem getAlertingSubsystem(
      CompletableFuture<AlertBreach> alertFuture) {
    AlertingSubsystem alertingSubsystem =
        AlertingSubsystem.create(alertFuture::complete, Executors.newSingleThreadExecutor());

    AlertingConfiguration config =
        AlertConfigParser.parse(
            "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400",
            "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400",
            "--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120",
            "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker");

    alertingSubsystem.initialize(config);
    return alertingSubsystem;
  }

  private static GcCollectionEvent mockGcEvent() {
    GcCollectionEvent event = Mockito.mock(GcCollectionEvent.class);
    GarbageCollector collector = Mockito.mock(GarbageCollector.class);
    MemoryPool tenuredPool = Mockito.mock(MemoryPool.class);
    Mockito.when(collector.isTenuredCollector()).thenReturn(true);
    Mockito.when(event.getCollector()).thenReturn(collector);
    Mockito.when(event.getTenuredPool()).thenReturn(Optional.of(tenuredPool));
    Mockito.when(event.getMemoryUsageAfterGc(Mockito.eq(tenuredPool)))
        .thenReturn(new MemoryUsage(1, 9, 10, 10));
    return event;
  }
}
