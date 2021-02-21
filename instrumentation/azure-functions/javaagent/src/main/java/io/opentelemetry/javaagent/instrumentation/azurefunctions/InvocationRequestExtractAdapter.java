/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// using reflection because these classes are not published to maven central that we can easily
// compile against
public class InvocationRequestExtractAdapter implements TextMapGetter<Object> {

  private static final Logger log = LoggerFactory.getLogger(InvocationRequestExtractAdapter.class);

  public static final InvocationRequestExtractAdapter GETTER =
      new InvocationRequestExtractAdapter();

  public static final Method getTraceContextMethod;
  private static final Method getTraceParentMethod;
  private static final Method getTraceStateMethod;

  static {
    Method getTraceContextMethodLocal = null;
    Method getTraceParentMethodLocal = null;
    Method getTraceStateMethodLocal = null;
    try {
      final Class<?> invocationRequestClass =
          Class.forName("com.microsoft.azure.functions.rpc.messages.InvocationRequest");
      final Class<?> rpcTraceContextClass =
          Class.forName("com.microsoft.azure.functions.rpc.messages.RpcTraceContext");
      getTraceContextMethodLocal = invocationRequestClass.getMethod("getTraceContext");
      getTraceParentMethodLocal = rpcTraceContextClass.getMethod("getTraceParent");
      getTraceStateMethodLocal = rpcTraceContextClass.getMethod("getTraceState");
    } catch (final ReflectiveOperationException e) {
      log.error(e.getMessage(), e);
    }
    getTraceContextMethod = getTraceContextMethodLocal;
    getTraceParentMethod = getTraceParentMethodLocal;
    getTraceStateMethod = getTraceStateMethodLocal;
  }

  @Override
  public Iterable<String> keys(Object carrier) {
    return null;
  }

  @Override
  public String get(final Object carrier, final String key) {
    try {
      // only supports W3C propagator
      switch (key) {
        case "traceparent":
          return (String) getTraceParentMethod.invoke(carrier);
        case "tracestate":
          return (String) getTraceStateMethod.invoke(carrier);
        default:
          return null;
      }
    } catch (final ReflectiveOperationException e) {
      log.debug(e.getMessage(), e);
      return null;
    }
  }
}
