/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.azure.monitor.opentelemetry.exporter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/** SpanProcessor implementation to update operation name. */
// note: operation name for requests is handled during export so that it can use the updated span
// name from routing instrumentation
//       if we (only) set operation name on requests here, it would be based on span name at
// startSpan
public class AiOperationNameSpanProcessor implements SpanProcessor {

  // visible for testing
  static final AttributeKey<String> AI_OPERATION_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operation_name");

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {

    // if user wants to change operation name, they should change operation name on the parent span
    // first before creating child span

    Span parentSpan = Span.fromContextOrNull(parentContext);
    if (parentSpan instanceof ReadableSpan) {
      span.setAttribute(AI_OPERATION_NAME_KEY, getOperationName((ReadableSpan) parentSpan));
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

  public static String getOperationName(ReadableSpan span) {
    String operationName = span.getAttribute(AI_OPERATION_NAME_KEY);
    if (operationName != null) {
      return operationName;
    }

    String spanName = span.getName();
    String httpMethod = span.getAttribute(SemanticAttributes.HTTP_METHOD);
    if (httpMethod != null && !httpMethod.isEmpty() && spanName.startsWith("/")) {
      return httpMethod + " " + spanName;
    }
    return spanName;
  }
}
