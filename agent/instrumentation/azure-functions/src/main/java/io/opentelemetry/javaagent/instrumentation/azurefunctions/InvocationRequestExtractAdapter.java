// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

// using reflection because these classes are not published to maven central that we can easily
// compile against
public class InvocationRequestExtractAdapter implements TextMapGetter<Object> {

  private static final Logger logger =
      Logger.getLogger(InvocationRequestExtractAdapter.class.getName());

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
      Class<?> invocationRequestClass =
          Class.forName("com.microsoft.azure.functions.rpc.messages.InvocationRequest");
      Class<?> rpcTraceContextClass =
          Class.forName("com.microsoft.azure.functions.rpc.messages.RpcTraceContext");
      getTraceContextMethodLocal = invocationRequestClass.getMethod("getTraceContext");
      getTraceParentMethodLocal = rpcTraceContextClass.getMethod("getTraceParent");
      getTraceStateMethodLocal = rpcTraceContextClass.getMethod("getTraceState");
    } catch (ReflectiveOperationException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
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
  @Nullable
  public String get(Object carrier, String key) {
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
    } catch (ReflectiveOperationException e) {
      logger.log(Level.FINE, e.getMessage(), e);
      return null;
    }
  }
}
