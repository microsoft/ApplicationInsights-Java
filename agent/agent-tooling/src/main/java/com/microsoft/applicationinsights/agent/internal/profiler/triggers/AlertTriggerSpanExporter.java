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

import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
import com.microsoft.applicationinsights.agent.internal.profiler.AlertingServiceFactory;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Intercepts spans, filters out those that have had alerts configured, and feeds them to an
 * appropriate AlertPipeline.
 */
public class AlertTriggerSpanExporter implements SpanExporter {
  // Next SpanExporter in App insights pipeline
  private final SpanExporter delegate;

  private final Supplier<AlertingSubsystem> alertingSubsystemSupplier;

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    spans.forEach(this::processSpan);
    return delegate.export(spans);
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  public void processSpan(SpanData span) {
    if (SpanDataMapper.isRequest(span)) {
      double durationInMillis =
          (span.getEndEpochNanos() - span.getStartEpochNanos()) / 1_000_000.0d;
      AlertingSubsystem alertingSubsystem = alertingSubsystemSupplier.get();

      if (alertingSubsystem != null) {
        alertingSubsystem.trackTelemetryDataPoint(
            TelemetryDataPoint.create(
                AlertMetricType.SPAN,
                TimeSource.DEFAULT.getNow(),
                span.getName(),
                durationInMillis));
      }
    }
  }

  public AlertTriggerSpanExporter(SpanExporter delegate) {
    this(delegate, AlertingServiceFactory::getAlertingSubsystem);
  }

  public AlertTriggerSpanExporter(
      SpanExporter delegate, Supplier<AlertingSubsystem> alertingSubsystemSupplier) {
    this.delegate = delegate;
    this.alertingSubsystemSupplier = alertingSubsystemSupplier;
  }
}
