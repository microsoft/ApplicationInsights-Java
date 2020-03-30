/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.servlet.javax;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import io.opentelemetry.instrumentation.servlet.ServletHttpServerTracer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class JavaxServletHttpServerTracer<RESPONSE>
    extends ServletHttpServerTracer<HttpServletRequest, RESPONSE> {
  public JavaxServletHttpServerTracer(JavaxServletAccessor<RESPONSE> accessor) {
    super(accessor);
  }

  public Context startSpan(
      HttpServletRequest request, HttpServletResponse response, String spanName, boolean servlet) {
    injectAppIdIntoResponse(response);
    return super.startSpan(request, spanName, servlet);
  }

  @Override
  protected TextMapGetter<HttpServletRequest> getGetter() {
    return JavaxHttpServletRequestGetter.GETTER;
  }

  @Override
  protected String errorExceptionAttributeName() {
    return "javax.servlet.error.exception";
  }

  private static void injectAppIdIntoResponse(HttpServletResponse response) {
    String appId = AiAppId.getAppId();
    if (!appId.isEmpty()) {
      response.setHeader(AiAppId.RESPONSE_HEADER_NAME, "appId=" + appId);
    }
  }
}
