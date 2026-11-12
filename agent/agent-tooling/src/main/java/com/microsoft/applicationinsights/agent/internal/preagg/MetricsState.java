// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.preagg;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientMetrics;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcServerMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerMetrics;
import java.lang.reflect.Field;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;

/**
 * Detects whether the upstream duration metric will be recorded for an operation.
 *
 * <p>Some instrumenters have http or rpc attributes but don't record the duration metrics (e.g. AWS
 * SDK, RMI). The upstream metrics store their per-operation state in the context in onStart() and
 * only record the duration in onEnd() when that state is present, so checking for the state tells
 * whether the pre-aggregated metric will be recorded.
 *
 * <p>The state is inherited by nested operations, so the state that was already in the parent
 * context is captured before the operation listeners run (see {@link #captureInherited}), and only
 * a state created by the current operation counts.
 */
enum MetricsState {
  HTTP_SERVER(HttpServerMetrics.class, "HTTP_SERVER_METRICS_STATE"),
  HTTP_CLIENT(HttpClientMetrics.class, "HTTP_CLIENT_REQUEST_METRICS_STATE"),
  RPC_SERVER(RpcServerMetrics.class, "RPC_SERVER_REQUEST_METRICS_STATE"),
  RPC_CLIENT(RpcClientMetrics.class, "RPC_CLIENT_REQUEST_METRICS_STATE");

  @SuppressWarnings("ImmutableEnumChecker") // ContextKey is immutable
  @Nullable
  private final ContextKey<?> contextKey;

  @SuppressWarnings("ImmutableEnumChecker") // ContextKey is immutable
  private final ContextKey<Inherited> inheritedContextKey;

  MetricsState(Class<?> metricsClass, String fieldName) {
    this.contextKey = lookupContextKey(metricsClass, fieldName);
    this.inheritedContextKey =
        ContextKey.named("applicationinsights.internal.inherited_metrics_state." + fieldName);
  }

  boolean isResolved() {
    return contextKey != null;
  }

  // needs to be called before the operation listeners' onStart()
  Context captureInherited(Context parentContext) {
    if (contextKey == null) {
      return parentContext;
    }
    return parentContext.with(inheritedContextKey, new Inherited(parentContext.get(contextKey)));
  }

  // identity comparison is intended, the upstream state is a value class
  @SuppressWarnings("ReferenceEquality")
  boolean isRecorded(Context context) {
    if (contextKey == null) {
      // fall back to assuming the metric will be recorded
      return true;
    }
    Object state = context.get(contextKey);
    if (state == null) {
      return false;
    }
    Inherited inherited = context.get(inheritedContextKey);
    // inherited is null when the operation was not started by a customized instrumenter
    return inherited == null || state != inherited.state;
  }

  @Nullable
  static ContextKey<?> lookupContextKey(Class<?> metricsClass, String fieldName) {
    try {
      Field field = metricsClass.getDeclaredField(fieldName);
      field.setAccessible(true);
      return (ContextKey<?>) field.get(null);
    } catch (ReflectiveOperationException | RuntimeException e) {
      // not using a static logger field, since this is called from the enum constructors which run
      // before the other static fields are initialized
      LoggerFactory.getLogger(MetricsState.class)
          .warn(
              "Unable to find {}.{}, pre-aggregated metrics may be inaccurate",
              metricsClass.getName(),
              fieldName,
              e);
      return null;
    }
  }

  private static final class Inherited {
    @Nullable private final Object state;

    private Inherited(@Nullable Object state) {
      this.state = state;
    }
  }
}
