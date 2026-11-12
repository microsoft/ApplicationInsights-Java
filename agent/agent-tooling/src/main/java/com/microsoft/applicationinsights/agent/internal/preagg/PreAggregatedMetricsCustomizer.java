// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.preagg;

import static io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer.InstrumentationType.HTTP_CLIENT;
import static io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer.InstrumentationType.HTTP_SERVER;
import static io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer.InstrumentationType.RPC_CLIENT;
import static io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer.InstrumentationType.RPC_SERVER;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.init.AiContextKeys;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.incubator.instrumenter.InstrumenterCustomizer;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.ContextCustomizer;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Adds the Application Insights specific attributes to the pre-aggregated http and rpc duration
 * metrics, and marks the span as pre-aggregated so that the exporter doesn't also generate standard
 * metrics from it.
 *
 * <p>The attributes are added in {@link #onEnd}, which runs right before the upstream metrics
 * record the duration with the same context and attributes.
 *
 * <p>Note: this only applies to the agent's instrumenters. Duration metrics recorded by
 * OpenTelemetry instrumentation libraries bundled in the application (through the OpenTelemetry API
 * bridge) only go through the views, so their spans are not marked as pre-aggregated.
 */
public final class PreAggregatedMetricsCustomizer
    implements ContextCustomizer<Object>, AttributesExtractor<Object, Object> {

  private static final ContextKey<Boolean> IS_SYNTHETIC =
      ContextKey.named("applicationinsights.internal.is_synthetic");

  private final List<MetricsState> metricsStates;
  private final boolean captureSynthetic;

  public static void customize(InstrumenterCustomizer customizer) {
    customize(customizer, SemconvStability.emitOldRpcSemconv());
  }

  // visible for testing
  static void customize(InstrumenterCustomizer customizer, boolean emitOldRpcSemconv) {
    List<MetricsState> metricsStates = new ArrayList<>();
    if (customizer.hasType(HTTP_SERVER)) {
      metricsStates.add(MetricsState.HTTP_SERVER);
    }
    if (customizer.hasType(HTTP_CLIENT)) {
      metricsStates.add(MetricsState.HTTP_CLIENT);
    }
    // with stable rpc semconv only, the upstream rpc metrics record rpc.*.call.duration instead of
    // rpc.*.duration, which are not pre-aggregated standard metrics
    if (emitOldRpcSemconv && customizer.hasType(RPC_SERVER)) {
      metricsStates.add(MetricsState.RPC_SERVER);
    }
    if (emitOldRpcSemconv && customizer.hasType(RPC_CLIENT)) {
      metricsStates.add(MetricsState.RPC_CLIENT);
    }
    if (metricsStates.isEmpty()) {
      return;
    }
    PreAggregatedMetricsCustomizer instance =
        new PreAggregatedMetricsCustomizer(
            metricsStates, metricsStates.contains(MetricsState.HTTP_SERVER));
    customizer.addContextCustomizer(instance);
    customizer.addAttributesExtractor(instance);
  }

  PreAggregatedMetricsCustomizer(List<MetricsState> metricsStates, boolean captureSynthetic) {
    this.metricsStates = metricsStates;
    this.captureSynthetic = captureSynthetic;
  }

  // context customizers run before the operation listeners' onStart(), and receive the start
  // attributes from all of the attributes extractors, which is needed to get the user agent
  @Override
  public Context onStart(Context parentContext, Object request, Attributes startAttributes) {
    Context context = parentContext;
    for (MetricsState metricsState : metricsStates) {
      context = metricsState.captureInherited(context);
    }
    if (captureSynthetic) {
      boolean synthetic = UserAgents.isBot(startAttributes);
      if (synthetic != Boolean.TRUE.equals(context.get(IS_SYNTHETIC))) {
        context = context.with(IS_SYNTHETIC, synthetic);
      }
    }
    return context;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, Object request) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Object request,
      @Nullable Object response,
      @Nullable Throwable error) {

    if (!isMetricRecorded(context)) {
      return;
    }

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    Span.fromContext(context).setAttribute(AiSemanticAttributes.IS_PRE_AGGREGATED, true);

    String connectionString = context.get(AiContextKeys.CONNECTION_STRING);
    if (connectionString != null) {
      // support for connectionStringOverrides
      // and for Classic SDK's setConnectionString()
      attributes.put(AiSemanticAttributes.INTERNAL_CONNECTION_STRING, connectionString);
    }
    String roleName = context.get(AiContextKeys.ROLE_NAME);
    if (roleName != null) {
      // support for roleNameOverrides and for Classic SDK's setConnectionString()
      // and Classic SDK set...
      attributes.put(AiSemanticAttributes.INTERNAL_ROLE_NAME, roleName);
    }
    if (captureSynthetic) {
      attributes.put(
          AiSemanticAttributes.IS_SYNTHETIC, Boolean.TRUE.equals(context.get(IS_SYNTHETIC)));
    }
  }

  private boolean isMetricRecorded(Context context) {
    for (MetricsState metricsState : metricsStates) {
      if (metricsState.isRecorded(context)) {
        return true;
      }
    }
    return false;
  }
}
