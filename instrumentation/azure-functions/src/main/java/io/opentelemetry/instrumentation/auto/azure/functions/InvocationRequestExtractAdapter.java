/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.auto.azure.functions;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// using reflection because these classes are not published to maven central that we can easily
// compile against
public class InvocationRequestExtractAdapter implements TextMapPropagator.Getter<Object> {

  private static final Logger log = LoggerFactory.getLogger(InvocationRequestExtractAdapter.class);

  public static final Tracer TRACER =
      OpenTelemetry.getGlobalTracer("io.opentelemetry.auto.azure-functions");

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
