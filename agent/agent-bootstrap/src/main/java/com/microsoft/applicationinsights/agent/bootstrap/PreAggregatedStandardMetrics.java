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

package com.microsoft.applicationinsights.agent.bootstrap;

import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_PRE_AGGREGATED;
import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_SYNTHETIC;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes;
import io.opentelemetry.instrumentation.api.instrumenter.UserAgents;
import javax.annotation.Nullable;

public class PreAggregatedStandardMetrics {

  @Nullable private static volatile AttributeGetter attributeGetter;

  public static void setAttributeGetter(AttributeGetter attributeGetter) {
    PreAggregatedStandardMetrics.attributeGetter = attributeGetter;
  }

  public static void applyHttpClientView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    Span span = Span.fromContext(context);
    applyCommon(builder, span);
  }

  public static void applyHttpServerView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    Span span = Span.fromContext(context);
    applyCommon(builder, span);

    // is_synthetic is only applied to server requests
    builder.put(IS_SYNTHETIC, UserAgents.isBot(endAttributes, startAttributes));
  }

  public static void applyRpcClientView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    applyHttpClientView(builder, context, startAttributes, endAttributes);
  }

  public static void applyRpcServerView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    applyHttpServerView(builder, context, startAttributes, endAttributes);
  }

  private static void applyCommon(AttributesBuilder builder, Span span) {

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    span.setAttribute(IS_PRE_AGGREGATED, true);

    if (attributeGetter == null) {
      return;
    }
    String connectionString =
        attributeGetter.get(span, BootstrapSemanticAttributes.CONNECTION_STRING);
    if (connectionString != null) {
      builder.put(BootstrapSemanticAttributes.CONNECTION_STRING, connectionString);
      return;
    }
    // back compat support
    String instrumentationKey =
        attributeGetter.get(span, BootstrapSemanticAttributes.INSTRUMENTATION_KEY);
    if (instrumentationKey != null) {
      builder.put(BootstrapSemanticAttributes.INSTRUMENTATION_KEY, instrumentationKey);
    }
  }

  @FunctionalInterface
  public interface AttributeGetter {
    <T> T get(Span span, AttributeKey<T> key);
  }

  private PreAggregatedStandardMetrics() {}
}
