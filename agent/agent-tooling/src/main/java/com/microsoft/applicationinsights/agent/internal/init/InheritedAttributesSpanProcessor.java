// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.List;
import java.util.stream.Collectors;

public final class InheritedAttributesSpanProcessor implements SpanProcessor {

  private final List<AttributeKey<?>> inheritAttributes;

  public InheritedAttributesSpanProcessor(
      List<Configuration.InheritedAttribute> inheritedAttributes) {
    this.inheritAttributes =
        inheritedAttributes.stream()
            .map(Configuration.InheritedAttribute::getAttributeKey)
            .collect(Collectors.toList());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Span parentSpan = Span.fromContextOrNull(parentContext);
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    for (AttributeKey<?> inheritAttributeKey : inheritAttributes) {
      Object value = parentReadableSpan.getAttribute(inheritAttributeKey);
      if (value != null) {
        span.setAttribute((AttributeKey<Object>) inheritAttributeKey, value);
      }
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
