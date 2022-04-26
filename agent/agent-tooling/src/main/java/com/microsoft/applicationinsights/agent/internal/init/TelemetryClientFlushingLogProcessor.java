package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogData;

public class TelemetryClientFlushingLogProcessor implements LogProcessor {

  private final LogProcessor delegate;
  private final TelemetryClient telemetryClient;

  public TelemetryClientFlushingLogProcessor(
      LogProcessor delegate, TelemetryClient telemetryClient) {
    this.delegate = delegate;
    this.telemetryClient = telemetryClient;
  }

  @Override
  public void emit(LogData logData) {
    delegate.emit(logData);
  }

  @Override
  public CompletableResultCode shutdown() {
    // see https://github.com/open-telemetry/opentelemetry-java/issues/4416
    return forceFlush();
  }

  @Override
  public CompletableResultCode forceFlush() {
    CompletableResultCode overallResult = new CompletableResultCode();
    CompletableResultCode delegateResult = delegate.forceFlush();
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

  @Override
  public void close() {
    delegate.close();
  }
}
