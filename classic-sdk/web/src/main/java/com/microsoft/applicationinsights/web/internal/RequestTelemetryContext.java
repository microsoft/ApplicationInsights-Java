// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.web.internal;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import javax.annotation.Nullable;

public final class RequestTelemetryContext {

  private final RequestTelemetry requestTelemetry = new RequestTelemetry();

  public RequestTelemetryContext(long ticks) {}

  public RequestTelemetry getHttpRequestTelemetry() {
    return requestTelemetry;
  }

  @Nullable
  public Tracestate getTracestate() {
    // Javaagent provides implementation
    return null;
  }

  public int getTraceflag() {
    // Javaagent provides implementation
    return 0;
  }
}
