/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
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

  @WithSpan
  private static void internalSetName() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setName("new name");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void setSuccess() {
    internalSetSuccess();
  }

  @WithSpan
  private static void internalSetSuccess() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setSuccess(false);
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void setSource() {
    internalSetSource();
  }

  @WithSpan
  private static void internalSetSource() {
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setSource("the source");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static String getId() {
    return internalGetId();
  }

  @WithSpan
  private static String internalGetId() {
    return ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getId();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static String getOperationId() {
    return internalGetOperationId();
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
  public static void setSessionId() {
    internalSetSessionId();
  }

  @WithSpan
  private static void internalSetSessionId() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getSession()
        .setId("the session id");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void setOperatingSystem() {
    internalSetOperatingSystem();
  }

  @WithSpan
  private static void internalSetOperatingSystem() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getDevice()
        .setOperatingSystem("the operating system");
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void setOperatingSystemVersion() {
    internalSetOperatingSystemVersion();
  }

  @WithSpan
  private static void internalSetOperatingSystemVersion() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getDevice()
        .setOperatingSystemVersion("the operating system version");
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
    ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getLocation();
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
  public static void otherSessionContextMethods() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getSession()
        .getId();
  }

  @WithSpan(kind = SpanKind.SERVER)
  public static void otherDeviceContextMethods() {
    ThreadContext.getRequestTelemetryContext()
        .getHttpRequestTelemetry()
        .getContext()
        .getDevice()
        .getId();
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

  private Code() {}
}
