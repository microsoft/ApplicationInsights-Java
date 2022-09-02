// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.web.internal;

import javax.annotation.Nullable;

public final class ThreadContext {

  @Nullable
  public static RequestTelemetryContext getRequestTelemetryContext() {
    // Javaagent provides implementation
    return null;
  }

  private ThreadContext() {}
}
