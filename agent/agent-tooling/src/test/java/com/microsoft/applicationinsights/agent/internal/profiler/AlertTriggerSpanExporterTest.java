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

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertTriggerSpanExporter;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.SpanAlertPipelineBuilder;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipelineMultiplexer;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AlertTriggerSpanExporterTest {

  interface Handle {
    void accept(AlertTriggerSpanExporter spanExporter, CountDownLatch alertCalled)
        throws InterruptedException;
  }

  @Test
  public void matchingFilterCausesAlert() throws InterruptedException {
    run(
        (spanExporter, alertCalled) -> {
          spanExporter.export(
              Collections.singletonList(
                  TestSpanData.builder()
                      .setName("fooBar")
                      .setStartEpochNanos(0L)
                      .setEndEpochNanos(2000000L)
                      .setHasEnded(true)
                      .setKind(SpanKind.SERVER)
                      .setStatus(StatusData.ok())
                      .build()));

          spanExporter.flush();

          Assertions.assertTrue(alertCalled.await(10, TimeUnit.SECONDS));
        });
  }

  @Test
  public void nonMatchingFilterDoesNotCauseAlert() throws InterruptedException {
    run(
        (spanExporter, alertCalled) -> {
          spanExporter.export(
              Collections.singletonList(
                  TestSpanData.builder()
                      .setName("does-not-match")
                      .setStartEpochNanos(0L)
                      .setEndEpochNanos(2000000L)
                      .setHasEnded(true)
                      .setKind(SpanKind.SERVER)
                      .setStatus(StatusData.ok())
                      .build()));

          spanExporter.flush();

          Assertions.assertFalse(alertCalled.await(100, TimeUnit.MILLISECONDS));
        });
  }

  static class StubAlertingSubsystem extends AlertingSubsystem {
    protected StubAlertingSubsystem(
        Consumer<AlertBreach> alertHandler, ExecutorService executorService) {
      super(alertHandler, executorService);
    }
  }

  private static void run(Handle handle) throws InterruptedException {
    ScheduledExecutorService executorService =
        Executors.newScheduledThreadPool(
            3,
            ThreadPoolUtils.createDaemonThreadFactory(
                AlertTriggerSpanExporter.class, "AlertSpanProcessor"));
    try {
      SpanExporter delegateSpanExporter = Mockito.mock(SpanExporter.class);

      CountDownLatch called = new CountDownLatch(1);
      Consumer<AlertBreach> alertAction = alertBreach -> called.countDown();

      Configuration.SpanTrigger triggerConfig = new Configuration.SpanTrigger();
      triggerConfig.filter.type = Configuration.SpanFilterType.REGEX;
      triggerConfig.filter.value = "foo.*";
      triggerConfig.threshold.value = 1;

      AlertingSubsystem alertingSubsystem = AlertingSubsystem.create(alertAction, executorService);

      alertingSubsystem.setPipeline(
          AlertMetricType.SPAN,
          new AlertPipelineMultiplexer(
              Arrays.asList(SpanAlertPipelineBuilder.build(triggerConfig, alertAction))));

      AlertTriggerSpanExporter spanExporter =
          new AlertTriggerSpanExporter(
              delegateSpanExporter, executorService, () -> alertingSubsystem);

      handle.accept(spanExporter, called);

    } finally {
      executorService.shutdownNow();
    }
  }
}
