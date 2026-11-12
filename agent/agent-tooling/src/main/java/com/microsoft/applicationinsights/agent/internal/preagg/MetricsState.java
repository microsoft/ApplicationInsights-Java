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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects whether the upstream duration metric will be recorded for an operation.
 *
 * <p>Some instrumenters have http or rpc attributes but don't record the duration metrics (e.g. AWS
 * SDK, RMI). The upstream metrics store their per-operation state in the context in onStart() and
 * only record the duration in onEnd() when that state is present, so checking for the state tells
 * whether the pre-aggregated metric will be recorded.
 */
enum MetricsState {
  HTTP_SERVER(HttpServerMetrics.class, "HTTP_SERVER_METRICS_STATE"),
  HTTP_CLIENT(HttpClientMetrics.class, "HTTP_CLIENT_REQUEST_METRICS_STATE"),
  RPC_SERVER(RpcServerMetrics.class, "RPC_SERVER_REQUEST_METRICS_STATE"),
  RPC_CLIENT(RpcClientMetrics.class, "RPC_CLIENT_REQUEST_METRICS_STATE");

  private static final Logger logger = LoggerFactory.getLogger(MetricsState.class);

  @SuppressWarnings("ImmutableEnumChecker") // ContextKey is immutable
  @Nullable
  private final ContextKey<?> contextKey;

  MetricsState(Class<?> metricsClass, String fieldName) {
    this.contextKey = lookupContextKey(metricsClass, fieldName);
  }

  boolean isResolved() {
    return contextKey != null;
  }

  boolean isPresent(Context context) {
    if (contextKey == null) {
      // fall back to assuming the metric will be recorded
      return true;
    }
    return context.get(contextKey) != null;
  }

  @Nullable
  private static ContextKey<?> lookupContextKey(Class<?> metricsClass, String fieldName) {
    try {
      Field field = metricsClass.getDeclaredField(fieldName);
      field.setAccessible(true);
      return (ContextKey<?>) field.get(null);
    } catch (ReflectiveOperationException | RuntimeException e) {
      logger.warn(
          "Unable to find {}.{}, pre-aggregated metrics may be inaccurate",
          metricsClass.getName(),
          fieldName,
          e);
      return null;
    }
  }
}
