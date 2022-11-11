// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import com.microsoft.azure.functions.rpc.messages.RpcTraceContext;
import io.opentelemetry.context.propagation.TextMapGetter;
import javax.annotation.Nullable;

public class InvocationRequestExtractAdapter implements TextMapGetter<RpcTraceContext> {

  public static final InvocationRequestExtractAdapter GETTER =
      new InvocationRequestExtractAdapter();

  @Override
  public Iterable<String> keys(RpcTraceContext carrier) {
    return null;
  }

  @Override
  @Nullable
  public String get(RpcTraceContext carrier, String key) {
    // only supports W3C propagator
    switch (key) {
      case "traceparent":
        return carrier.getTraceParent();
      case "tracestate":
        return carrier.getTraceState();
      default:
        return null;
    }
  }
}
