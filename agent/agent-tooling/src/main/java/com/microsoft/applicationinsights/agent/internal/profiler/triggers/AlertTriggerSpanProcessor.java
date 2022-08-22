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
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Supplier;

/**
 * Intercepts spans, filters out those that have had alerts configured, and feeds them to an
 * appropriate AlertPipeline.
 */
public class AlertTriggerSpanProcessor implements SpanProcessor {

  private final Supplier<AlertingSubsystem> alertingSubsystemSupplier;

  public AlertTriggerSpanProcessor() {
    this(AlertingServiceFactory::getAlertingSubsystem);
  }

  public AlertTriggerSpanProcessor(Supplier<AlertingSubsystem> alertingSubsystemSupplier) {
    this.alertingSubsystemSupplier = alertingSubsystemSupplier;
  }

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {}

  @Override
  public boolean isEndRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    processSpan(span);
  }

  public void processSpan(ReadableSpan span) {
    if (SpanDataMapper.isRequest(span)) {
      double durationInMillis = span.getLatencyNanos() / 1_000_000.0d;
      AlertingSubsystem alertingSubsystem = alertingSubsystemSupplier.get();

      if (alertingSubsystem != null) {
        alertingSubsystem.trackTelemetryDataPoint(
            TelemetryDataPoint.create(
                AlertMetricType.REQUEST,
                TimeSource.DEFAULT.getNow(),
                span.getName(),
                durationInMillis));
      }
    }
  }
}
