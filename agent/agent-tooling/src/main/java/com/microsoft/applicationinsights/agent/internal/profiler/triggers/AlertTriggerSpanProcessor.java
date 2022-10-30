// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.azure.monitor.opentelemetry.exporter.implementation.RequestChecker;
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
    this(AlertingSubsystemInit::getAlertingSubsystem);
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
    if (RequestChecker.isRequest(span)) {
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
