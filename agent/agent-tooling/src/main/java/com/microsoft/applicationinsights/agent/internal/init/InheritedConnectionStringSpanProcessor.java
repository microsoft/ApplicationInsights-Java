// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;

public class InheritedConnectionStringSpanProcessor implements SpanProcessor {

  private final List<Configuration.ConnectionStringOverride> overrides;

  public InheritedConnectionStringSpanProcessor(
      List<Configuration.ConnectionStringOverride> overrides) {

    this.overrides = overrides;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();
    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      // this part (setting the attribute on the local root span) could be moved to Sampler
      String target = span.getAttribute(SemanticAttributes.HTTP_TARGET);
      if (target == null) {
        return;
      }
      for (Configuration.ConnectionStringOverride override : overrides) {
        if (target.startsWith(override.httpPathPrefix)) {
          span.setAttribute(AiSemanticAttributes.CONNECTION_STRING, override.connectionString);
          break;
        }
      }
      return;
    }
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    String connectionString =
        parentReadableSpan.getAttribute(AiSemanticAttributes.CONNECTION_STRING);
    if (connectionString != null) {
      span.setAttribute(AiSemanticAttributes.CONNECTION_STRING, connectionString);
    } else {
      // back compat support
      String instrumentationKey =
          parentReadableSpan.getAttribute(AiSemanticAttributes.INSTRUMENTATION_KEY);
      if (instrumentationKey != null) {
        span.setAttribute(AiSemanticAttributes.INSTRUMENTATION_KEY, instrumentationKey);
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
