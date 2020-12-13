/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import application.io.opentelemetry.api.trace.Span;
import application.io.opentelemetry.extension.annotations.WithSpan;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

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
  public void updateName() {
    internalUpdateName();
  }

  @WithSpan
  private void internalUpdateName() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setName("new name");
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
}
