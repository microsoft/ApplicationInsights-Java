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

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.ArrayList;
import java.util.List;

public class InheritedAttributesLogProcessor implements LogProcessor {

  private static final AttributeKey<String> INSTRUMENTATION_KEY_KEY =
      AttributeKey.stringKey("ai.preview.instrumentation_key");

  private static final AttributeKey<String> ROLE_NAME_KEY =
      AttributeKey.stringKey("ai.preview.service_name");

  private final List<AttributeKey<?>> inheritedAttributes;

  public InheritedAttributesLogProcessor(
      List<Configuration.InheritedAttribute> inheritedAttributes) {
    this.inheritedAttributes = buildInheritedAttributesList(inheritedAttributes);
  }

  private static List<AttributeKey<?>> buildInheritedAttributesList(
      List<Configuration.InheritedAttribute> inheritedAttributes) {
    List<AttributeKey<?>> list = new ArrayList<>();
    for (Configuration.InheritedAttribute inheritedAttribute : inheritedAttributes) {
      list.add(inheritedAttribute.getAttributeKey());
    }
    list.add(INSTRUMENTATION_KEY_KEY);
    list.add(ROLE_NAME_KEY);
    return list;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onEmit(ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.current();
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

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public void close() {}
}
