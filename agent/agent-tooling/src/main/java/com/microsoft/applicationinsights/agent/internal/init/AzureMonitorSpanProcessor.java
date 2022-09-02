// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.OperationNames;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

// note: operation name for requests is handled during export so that it can use the updated span
// name from routing instrumentation
//       if we (only) set operation name on requests here, it would be based on span name at
// startSpan
public class AzureMonitorSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {

    // if user wants to change operation name, they should change operation name on the parent span
    // first before creating child span

    Span parentSpan = Span.fromContextOrNull(parentContext);
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    span.setAttribute(
        AiSemanticAttributes.OPERATION_NAME, OperationNames.getOperationName(parentReadableSpan));
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
