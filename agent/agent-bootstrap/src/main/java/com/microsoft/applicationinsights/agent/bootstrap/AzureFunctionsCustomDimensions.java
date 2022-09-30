// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ImplicitContextKeyed;

public class AzureFunctionsCustomDimensions implements ImplicitContextKeyed {

  private static final ContextKey<AzureFunctionsCustomDimensions>
      AI_FUNCTION_CUSTOM_DIMENSIONS_KEY = ContextKey.named("ai-function-custom-dimensions");

  public final String invocationId;
  public final String processId;
  public final String logLevel;
  public final String category;
  public final String hostInstanceId;
  public final String azFunctionLiveLogsSessionId;

  public AzureFunctionsCustomDimensions(
      String invocationId,
      String processId,
      String logLevel,
      String category,
      String hostInstanceId,
      String azFunctionLiveLogsSessionId) {
    this.invocationId = invocationId;
    this.processId = processId;
    this.logLevel = logLevel;
    this.category = category;
    this.hostInstanceId = hostInstanceId;
    this.azFunctionLiveLogsSessionId = azFunctionLiveLogsSessionId;
  }

  public static AzureFunctionsCustomDimensions fromContext(Context context) {
    return context.get(AI_FUNCTION_CUSTOM_DIMENSIONS_KEY);
  }

  @Override
  public Context storeInContext(Context context) {
    return context.with(AI_FUNCTION_CUSTOM_DIMENSIONS_KEY, this);
  }

  // TODO to be removed and it's for debugging
  public String toString() {
    return "{invocationId:"
        + invocationId
        + ", processId:"
        + processId
        + ", logLevel:"
        + logLevel
        + ", category:"
        + category
        + ", hostInstanceId:"
        + hostInstanceId
        + ", azFunctionLiveLogSessionId:"
        + azFunctionLiveLogsSessionId
        + "}";
  }
}
