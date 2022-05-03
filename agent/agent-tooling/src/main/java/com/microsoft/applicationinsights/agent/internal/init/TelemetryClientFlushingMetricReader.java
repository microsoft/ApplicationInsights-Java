package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;

public class TelemetryClientFlushingMetricReader implements MetricReader {

  private final MetricReader delegate;
  private final TelemetryClient telemetryClient;

  public TelemetryClientFlushingMetricReader(
      MetricReader delegate, TelemetryClient telemetryClient) {
    this.delegate = delegate;
    this.telemetryClient = telemetryClient;
  }

  @Override
  public void register(CollectionRegistration registration) {
    delegate.register(registration);
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return delegate.getAggregationTemporality(instrumentType);
  }

  @Override
  public CompletableResultCode shutdown() {

    // TODO? de-dupe flushing of TelemetryClient three times for spans, logs and metrics

    return flush();
  }

  @Override
  public CompletableResultCode flush() {
    CompletableResultCode overallResult = new CompletableResultCode();
    CompletableResultCode delegateResult = delegate.flush();
    delegateResult.whenComplete(
        () -> {
          if (delegateResult.isSuccess()) {
            CompletableResultCode telemetryClientResult = telemetryClient.forceFlush();
            telemetryClientResult.whenComplete(
                () -> {
                  if (telemetryClientResult.isSuccess()) {
                    overallResult.succeed();
                  } else {
                    overallResult.fail();
                  }
                });
          } else {
            overallResult.fail();
          }
        });
    return overallResult;
  }
}
