/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.extension.annotations.WithSpan;

public class Code {

  @WithSpan(kind = Span.Kind.SERVER)
  public void setProperty() {
    internalSetProperty();
  }

  @WithSpan
  private void internalSetProperty() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getProperties()
        .put("akey", "avalue");
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void setUser() {
    internalSetUser();
  }

  @WithSpan
  private void internalSetUser() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getUser()
        .setId("myuser");
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void setName() {
    internalSetName();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public String getId() {
    return internalGetId();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public String getOperationId() {
    return internalGetOperationId();
  }

  @WithSpan
  private void internalSetName() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setName("new name");
  }

  @WithSpan
  private String internalGetId() {
    return ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getId();
  }

  @WithSpan
  private String internalGetOperationId() {
    return ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getOperation()
        .getId();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void otherRequestTelemetryContextMethods() {
    ThreadContext.getRequestTelemetryContext().getCorrelationContext();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void otherRequestTelemetryMethods() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getName();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void otherBaseTelemetryMethods() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getTimestamp();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void otherTelemetryContextMethods() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getSession();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void otherUserContextMethods() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getUser()
        .setAccountId("abc");
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public void otherOperationContextMethods() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getOperation()
        .setId("xyz");
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public Tracestate getTracestate() {
    return ThreadContext.getRequestTelemetryContext().getTracestate();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public int getTraceflag() {
    return ThreadContext.getRequestTelemetryContext().getTraceflag();
  }

  @WithSpan(kind = Span.Kind.SERVER)
  public String retriveTracestate() {
    return TraceContextCorrelation.retriveTracestate();
  }
}
