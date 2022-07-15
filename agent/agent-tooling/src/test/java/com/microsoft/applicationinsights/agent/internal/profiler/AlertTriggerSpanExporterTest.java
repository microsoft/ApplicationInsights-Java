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

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.AlertTriggerSpanExporter;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.RequestAlertPipelineBuilder;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipelineMultiplexer;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AlertTriggerSpanExporterTest {

  interface Handle {
    void accept(
        AlertTriggerSpanExporter spanExporter,
        AtomicBoolean alertCalled,
        TestTimeSource timeSource)
        throws InterruptedException;
  }

  @Test
  public void matchingFilterCausesAlert() throws InterruptedException {
    run(
        (spanExporter, alertCalled, timeSource) -> {
          for (int i = 0; i < 10; i++) {
            spanExporter.export(buildSampleSpan("fooBar", 20000000000L));
            timeSource.increment(10000);
          }

          spanExporter.flush();

          Assertions.assertTrue(alertCalled.get());
        });
  }

  @Test
  public void underThresholdDataDoesNotCauseAlert() throws InterruptedException {
    run(
        (spanExporter, alertCalled, timeSource) -> {
          for (int i = 0; i < 100000; i += 10) {
            timeSource.setNow(Instant.EPOCH.plus(i, ChronoUnit.MILLIS));
            spanExporter.export(buildSampleSpan("fooBar", 2000000000L));
          }

          spanExporter.flush();

          Assertions.assertFalse(alertCalled.get());
        });
  }

  @Test
  public void underThenOverThresholdDataDoesCauseAlert() throws InterruptedException {
    run(
        (spanExporter, alertCalled, timeSource) -> {
          for (int i = 0; i < 100000; i += 10) {
            timeSource.setNow(Instant.EPOCH.plus(i, ChronoUnit.MILLIS));
            spanExporter.export(buildSampleSpan("fooBar", 2000000000L));
          }

          spanExporter.flush();

          Assertions.assertFalse(alertCalled.get());

          for (int i = 100000; i < 200000; i += 10) {
            timeSource.setNow(Instant.EPOCH.plus(i, ChronoUnit.MILLIS));
            spanExporter.export(buildSampleSpan("fooBar", 200000000000L));
          }

          spanExporter.flush();

          Assertions.assertTrue(alertCalled.get());
        });
  }

  @NotNull
  private static List<SpanData> buildSampleSpan(String fooBar, long epochNanos) {
    return Collections.singletonList(
        TestSpanData.builder()
            .setName(fooBar)
            .setStartEpochNanos(0L)
            .setEndEpochNanos(epochNanos)
            .setHasEnded(true)
            .setKind(SpanKind.SERVER)
            .setStatus(StatusData.ok())
            .build());
  }

  @Test
  public void nonMatchingFilterDoesNotCauseAlert() throws InterruptedException {
    run(
        (spanExporter, alertCalled, timeSource) -> {
          for (int i = 0; i < 10; i++) {
            spanExporter.export(buildSampleSpan("does-not-match", 20000000000L));
            timeSource.increment(10000);
          }

          spanExporter.flush();

          Assertions.assertFalse(alertCalled.get());
        });
  }

  private static void run(Handle handle) throws InterruptedException {
    SpanExporter delegateSpanExporter = Mockito.mock(SpanExporter.class);

    AtomicBoolean called = new AtomicBoolean(false);
    Consumer<AlertBreach> alertAction =
        alertBreach -> {
          called.set(true);
        };

    Configuration.RequestTrigger triggerConfig = new Configuration.RequestTrigger();
    triggerConfig.filter.type = Configuration.RequestFilterType.NAME_REGEX;
    triggerConfig.filter.value = "foo.*";
    triggerConfig.threshold.value = 0.75f;

    AlertingSubsystem alertingSubsystem = AlertingSubsystem.create(alertAction, TimeSource.DEFAULT);

    TestTimeSource timeSource = new TestTimeSource();
    timeSource.setNow(Instant.EPOCH);

    alertingSubsystem.setPipeline(
        AlertMetricType.REQUEST,
        new AlertPipelineMultiplexer(
            Arrays.asList(
                RequestAlertPipelineBuilder.build(triggerConfig, alertAction, timeSource))));

    AlertTriggerSpanExporter spanExporter =
        new AlertTriggerSpanExporter(delegateSpanExporter, () -> alertingSubsystem);

    handle.accept(spanExporter, called, timeSource);
  }
}
