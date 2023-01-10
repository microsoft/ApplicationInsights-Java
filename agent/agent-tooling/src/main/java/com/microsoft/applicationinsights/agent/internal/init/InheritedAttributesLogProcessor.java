// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.List;
import java.util.stream.Collectors;

public final class InheritedAttributesLogProcessor implements LogRecordProcessor {

  private final List<AttributeKey<?>> inheritedAttributes;

  public InheritedAttributesLogProcessor(
      List<Configuration.InheritedAttribute> inheritedAttributes) {
    this.inheritedAttributes =
        inheritedAttributes.stream()
            .map(Configuration.InheritedAttribute::getAttributeKey)
            .collect(Collectors.toList());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.fromContext(context);
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) currentSpan;
    for (AttributeKey<?> inheritedAttributeKey : inheritedAttributes) {
      Object value = readableSpan.getAttribute(inheritedAttributeKey);
      if (value != null) {
        logRecord.setAttribute((AttributeKey<Object>) inheritedAttributeKey, value);
      }
    }
  }
}
