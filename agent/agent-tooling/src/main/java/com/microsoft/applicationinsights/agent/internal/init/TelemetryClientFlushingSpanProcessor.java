package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class TelemetryClientFlushingSpanProcessor implements SpanProcessor {

  private final SpanProcessor delegate;
  private final TelemetryClient telemetryClient;

  public TelemetryClientFlushingSpanProcessor(
      SpanProcessor delegate, TelemetryClient telemetryClient) {
    this.delegate = delegate;
    this.telemetryClient = telemetryClient;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    delegate.onStart(parentContext, span);
  }

  @Override
  public boolean isStartRequired() {
    return delegate.isStartRequired();
  }

  @Override
  public void onEnd(ReadableSpan span) {
    delegate.onEnd(span);
  }

  @Override
  public boolean isEndRequired() {
    return delegate.isEndRequired();
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
