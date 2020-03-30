/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.servlet.v2_2;

import static io.opentelemetry.instrumentation.api.servlet.ServerSpanNaming.Source.SERVLET;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import io.opentelemetry.instrumentation.api.servlet.ServerSpanNaming;
import io.opentelemetry.instrumentation.servlet.javax.JavaxServletHttpServerTracer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlet2HttpServerTracer extends JavaxServletHttpServerTracer<ResponseWithStatus> {
  private static final Servlet2HttpServerTracer TRACER = new Servlet2HttpServerTracer();

  public Servlet2HttpServerTracer() {
    super(Servlet2Accessor.INSTANCE);
  }

  public static Servlet2HttpServerTracer tracer() {
    return TRACER;
  }

  public Context startSpan(HttpServletRequest request, HttpServletResponse response) {
    injectAppIdIntoResponse(response);
    return startSpan(request, getSpanName(request), true);
  }

  @Override
  public Context updateContext(Context context, HttpServletRequest request) {
    ServerSpanNaming.updateServerSpanName(context, SERVLET, () -> getSpanName(request));
    return super.updateContext(context, request);
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.servlet-2.2";
  }

  private static void injectAppIdIntoResponse(HttpServletResponse response) {
    String appId = AiAppId.getAppId();
    if (!appId.isEmpty()) {
      response.setHeader(AiAppId.RESPONSE_HEADER_NAME, "appId=" + appId);
    }
  }
}
