/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.extension.annotations.WithSpan;

public class Code {

  @WithSpan(kind = SpanKind.SERVER)
  public static void setProperty() {
    internalSetProperty();
  }

  @WithSpan
  private static void internalSetProperty() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getProperties()
        .put("akey", "avalue");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void setUser() {
    internalSetUser();
  }

  @WithSpan
  private static void internalSetUser() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getUser()
        .setId("myuser");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void setName() {
    internalSetName();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void setSource() {
    internalSetSource();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static String getId() {
    return internalGetId();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static String getOperationId() {
    return internalGetOperationId();
  }

  @WithSpan
  private static void internalSetName() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setName("new name");
  }

  @WithSpan
  private static void internalSetSource() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setSource("the source");
  }

  @WithSpan
  private static String internalGetId() {
    return ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getId();
  }

  @WithSpan
  private static String internalGetOperationId() {
    return ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getOperation()
        .getId();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void otherRequestTelemetryContextMethods() {
    ThreadContext.getRequestTelemetryContext().getCorrelationContext();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void otherRequestTelemetryMethods() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getName();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void otherBaseTelemetryMethods() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getTimestamp();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void otherTelemetryContextMethods() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getSession();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void otherUserContextMethods() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getUser()
        .setAccountId("abc");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void otherOperationContextMethods() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getOperation()
        .setId("xyz");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static Tracestate getTracestate() {
    return ThreadContext.getRequestTelemetryContext().getTracestate();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static int getTraceflag() {
    return ThreadContext.getRequestTelemetryContext().getTraceflag();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static String retriveTracestate() {
    return TraceContextCorrelation.retriveTracestate();
  }
}
