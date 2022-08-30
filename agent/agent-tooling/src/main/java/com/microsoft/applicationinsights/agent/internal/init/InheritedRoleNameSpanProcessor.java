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

public class InheritedRoleNameSpanProcessor implements SpanProcessor {

  private final List<Configuration.RoleNameOverride> overrides;

  public InheritedRoleNameSpanProcessor(List<Configuration.RoleNameOverride> overrides) {
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
      for (Configuration.RoleNameOverride override : overrides) {
        if (target.startsWith(override.httpPathPrefix)) {
          span.setAttribute(AiSemanticAttributes.ROLE_NAME, override.roleName);
          break;
        }
      }
      return;
    }
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    String roleName = parentReadableSpan.getAttribute(AiSemanticAttributes.ROLE_NAME);
    if (roleName != null) {
      span.setAttribute(AiSemanticAttributes.ROLE_NAME, roleName);
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
