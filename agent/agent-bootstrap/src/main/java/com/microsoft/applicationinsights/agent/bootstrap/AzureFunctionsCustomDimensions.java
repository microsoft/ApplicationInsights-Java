// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ImplicitContextKeyed;

public final class AzureFunctionsCustomDimensions implements ImplicitContextKeyed {

  private static final ContextKey<AzureFunctionsCustomDimensions>
      AI_FUNCTIONS_CUSTOM_DIMENSIONS_KEY = ContextKey.named("ai-functions-custom-dimensions");

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
    return context.get(AI_FUNCTIONS_CUSTOM_DIMENSIONS_KEY);
  }

  @Override
  public Context storeInContext(Context context) {
    return context.with(AI_FUNCTIONS_CUSTOM_DIMENSIONS_KEY, this);
  }
}
