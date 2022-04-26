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
    System.out.println("FORCE FLUSH");
    delegateResult.whenComplete(
        () -> {
          System.out.println("DELEGATE COMPLETE");
          if (delegateResult.isSuccess()) {
            System.out.println("SUCCESS");
            CompletableResultCode telemetryClientResult = telemetryClient.forceFlush();
            telemetryClientResult.whenComplete(
                () -> {
                  System.out.println("TELEMETRY CLIENT COMPLETE");
                  if (telemetryClientResult.isSuccess()) {
                    System.out.println("TELEMETRY CLIENT SUCCESS");
                    overallResult.succeed();
                  } else {
                    System.out.println("TELEMETRY CLIENT FAIL");
                    overallResult.fail();
                  }
                });
          } else {
            System.out.println("FAIL");
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
