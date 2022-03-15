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

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.AiOperationNameSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.processors.MyLogData;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.ArrayList;
import java.util.List;

public class InheritedAttributesLogProcessor implements LogProcessor {

  private static final AttributeKey<String> AI_OPERATION_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operation_name");

  private static final AttributeKey<String> INSTRUMENTATION_KEY_KEY =
      AttributeKey.stringKey("ai.preview.instrumentation_key");

  private static final AttributeKey<String> ROLE_NAME_KEY =
      AttributeKey.stringKey("ai.preview.service_name");

  private final List<AttributeKey<?>> inheritedAttributes;
  private final LogProcessor delegate;

  public InheritedAttributesLogProcessor(
      List<Configuration.InheritedAttribute> inheritedAttributes, LogProcessor delegate) {
    this.inheritedAttributes = buildInheritedAttributesList(inheritedAttributes);
    this.delegate = delegate;
  }

  private static List<AttributeKey<?>> buildInheritedAttributesList(
      List<Configuration.InheritedAttribute> inheritedAttributes) {
    List<AttributeKey<?>> list = new ArrayList<>();
    for (Configuration.InheritedAttribute inheritedAttribute : inheritedAttributes) {
      list.add(inheritedAttribute.getAttributeKey());
    }
    list.add(AI_OPERATION_NAME_KEY);
    list.add(INSTRUMENTATION_KEY_KEY);
    list.add(ROLE_NAME_KEY);
    return list;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void emit(LogData log) {
    Span currentSpan = Span.current();
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) currentSpan;
    AttributesBuilder builder = log.getAttributes().toBuilder();
    for (AttributeKey<?> inheritedAttributeKey : inheritedAttributes) {
      Object value = readableSpan.getAttribute(inheritedAttributeKey);
      if (inheritedAttributeKey == AI_OPERATION_NAME_KEY) {
        value = AiOperationNameSpanProcessor.getOperationName(readableSpan);
      }
      if (value != null) {
        if (builder == null) {
          builder = log.getAttributes().toBuilder();
        }
        builder.put((AttributeKey<Object>) inheritedAttributeKey, value);
      }
    }
    if (builder != null) {
      log = new MyLogData(log, builder.build());
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
