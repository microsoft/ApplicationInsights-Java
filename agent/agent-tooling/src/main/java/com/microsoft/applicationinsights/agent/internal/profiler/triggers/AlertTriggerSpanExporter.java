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

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.profiler.AlertingServiceFactory;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepts spans, filters out those that have had alerts configured, and feeds them to an
 * appropriate AlertPipeline.
 */
public class AlertTriggerSpanExporter implements SpanExporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertTriggerSpanExporter.class);

  // Next SpanExporter in App insights pipeline
  private final SpanExporter delegate;

  // Execution context of the execution loop
  private final Future<?> spanProcessor;

  // Spans to be processed
  private final LinkedBlockingQueue<SpanData> queue = new LinkedBlockingQueue<>();
  private final Supplier<AlertingSubsystem> alertingSubsystemSupplier;

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    // Process spans off of the main thread to avoid any complex logic blocking span processing
    spans.forEach(queue::offer);

    return delegate.export(spans);
  }

  @Override
  public CompletableResultCode flush() {
    int maxWaitCount = 10;
    while (queue.size() > 0 && maxWaitCount-- > 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    spanProcessor.cancel(false);
    return delegate.shutdown();
  }

  /** Runnable that processes incoming spans off of the main thread. */
  private class ProcessSpans implements Runnable {
    @Override
    public void run() {
      while (true) {
        try {
          SpanData span = queue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
          if (span != null) {
            processSpan(span);
          }
        } catch (RuntimeException e) {
          // Avoid killing polling due to a single exception
          LOGGER.error("Error while processing span: {}", e.getMessage());
        } catch (InterruptedException e) {
          return;
        }
      }
    }
  }

  public void processSpan(SpanData span) {
    if (span.getKind() == SpanKind.SERVER) {
      double duration = (span.getEndEpochNanos() - span.getStartEpochNanos()) / 1_000_000.0d;
      AlertingSubsystem alertingSubsystem = alertingSubsystemSupplier.get();

      if (alertingSubsystem != null) {
        alertingSubsystem.trackTelemetryDataPoint(
            new TelemetryDataPoint(
                AlertMetricType.SPAN, TimeSource.DEFAULT.getNow(), span.getName(), duration));
      }
    }
  }

  public static AlertTriggerSpanExporter build(SpanExporter delegate) {
    ScheduledExecutorService executorService =
        Executors.newSingleThreadScheduledExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(
                AlertTriggerSpanExporter.class, "AlertSpanProcessor"));
    return new AlertTriggerSpanExporter(delegate, executorService);
  }

  public AlertTriggerSpanExporter(SpanExporter delegate, ExecutorService executorService) {
    this(delegate, executorService, AlertingServiceFactory::getAlertingSubsystem);
  }

  public AlertTriggerSpanExporter(
      SpanExporter delegate,
      ExecutorService executorService,
      Supplier<AlertingSubsystem> alertingSubsystemSupplier) {
    this.delegate = delegate;
    this.spanProcessor = executorService.submit(new ProcessSpans());
    this.alertingSubsystemSupplier = alertingSubsystemSupplier;
  }
}
