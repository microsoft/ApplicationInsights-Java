package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.processors.MyLogData;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.List;
import java.util.stream.Collectors;

public class InheritedAttributesLogProcessor implements LogProcessor {

  private final List<AttributeKey<?>> inheritedAttributes;
  private final LogProcessor delegate;

  public InheritedAttributesLogProcessor(List<Configuration.InheritedAttribute> inheritedAttributes, LogProcessor delegate) {
    this.inheritedAttributes = inheritedAttributes.stream()
        .map(Configuration.InheritedAttribute::getAttributeKey)
        .collect(Collectors.toList());
    this.delegate = delegate;
  }

  @SuppressWarnings("SystemOut")
  @Override
  public void emit(LogData logData) {
    Span currentSpan = Span.current();
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) currentSpan;
    for (AttributeKey<?> inheritedAttributeKey : inheritedAttributes) {
      Object value = readableSpan.getAttribute(inheritedAttributeKey);
      System.out.println("############## inherited key: " + inheritedAttributeKey.getKey());
      System.out.println("############## inherited value: " + value.toString());
      if (value != null) {
        logData.getAttributes().toBuilder().put((AttributeKey<Object>) inheritedAttributeKey, value);
      }
    }

    logData = new MyLogData(logData, logData.getAttributes());
    delegate.emit(logData);
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
