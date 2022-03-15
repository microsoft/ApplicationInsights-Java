package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.processors.MyLogData;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class InheritedRoleNameLogProcessor implements LogProcessor {

  private static final AttributeKey<String> ROLE_NAME_KEY =
      AttributeKey.stringKey("ai.preview.service_name");

  private final LogProcessor delegate;

  public InheritedRoleNameLogProcessor(LogProcessor delegate) {
    this.delegate = delegate;
  }

  @Override
  public void emit(LogData log) {
    Span span = Span.current();
    if (!(span instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) span;
    String roleName = readableSpan.getAttribute(ROLE_NAME_KEY);
    if (roleName != null) {
      log = new MyLogData(
          log,
          log.getAttributes().toBuilder()
              .put(ROLE_NAME_KEY, roleName)
              .build());
    }

    delegate.emit(log);
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return delegate.forceFlush();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
