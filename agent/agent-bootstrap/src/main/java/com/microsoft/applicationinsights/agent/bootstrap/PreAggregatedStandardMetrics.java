// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_PRE_AGGREGATED;
import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_SYNTHETIC;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.UserAgents;

public class PreAggregatedStandardMetrics {

  public static void applyHttpClientView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    Span.fromContext(context).setAttribute(IS_PRE_AGGREGATED, true);
  }

  public static void applyHttpServerView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    Span.fromContext(context).setAttribute(IS_PRE_AGGREGATED, true);

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

  private PreAggregatedStandardMetrics() {}
}
