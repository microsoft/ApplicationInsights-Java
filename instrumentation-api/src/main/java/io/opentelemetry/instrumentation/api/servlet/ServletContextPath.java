/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.servlet;

import io.grpc.Context;

/**
 * The context key here is used to propagate the servlet context path throughout the request, so
 * that routing framework instrumentation that updates the span name with a more specific route can
 * prepend the servlet context path in front of that route.
 *
 * <p>This needs to be in the instrumentation-api module, instead of injected as a helper class into
 * the different modules that need it, in order to make sure that there is only a single instance of
 * the context key, since otherwise instrumentation across different class loaders would use
 * different context keys and not be able to share the servlet context path.
 */
public class ServletContextPath {

  // Keeps track of the servlet context path that needs to be prepended to the route when updating
  // the span name
  public static final Context.Key<String> CONTEXT_KEY =
      Context.key("opentelemetry-servlet-context-path-key");

  public static String prepend(Context context, String spanName) {
    String value = CONTEXT_KEY.get(context);
    // checking isEmpty just to avoid unnecessary string concat / allocation
    if (value != null && !value.isEmpty()) {
      return value + spanName;
    } else {
      return spanName;
    }
  }
}
