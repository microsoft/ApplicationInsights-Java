// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctionsCustomDimensions;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;

public final class AzureFunctionsLogProcessor implements LogRecordProcessor {

  private static final ClientLogger logger = new ClientLogger(AzureFunctionsLogProcessor.class);

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    AzureFunctionsCustomDimensions customDimensions =
        AzureFunctionsCustomDimensions.fromContext(context);
    if (customDimensions == null) {
      logger.verbose("'ai-functions-custom-dimensions' is missing from the context");
      return;
    }
    if (customDimensions.invocationId != null) {
      logRecord.setAttribute(
          AiSemanticAttributes.AZ_FN_INVOCATION_ID, customDimensions.invocationId);
    }
    if (customDimensions.processId != null) {
      logRecord.setAttribute(AiSemanticAttributes.AZ_FN_PROCESS_ID, customDimensions.processId);
    }
    if (customDimensions.logLevel != null) {
      logRecord.setAttribute(AiSemanticAttributes.AZ_FN_LOG_LEVEL, customDimensions.logLevel);
    }
    if (customDimensions.category != null) {
      logRecord.setAttribute(AiSemanticAttributes.AZ_FN_CATEGORY, customDimensions.category);
    }
    if (customDimensions.hostInstanceId != null) {
      logRecord.setAttribute(
          AiSemanticAttributes.AZ_FN_HOST_INSTANCE_ID, customDimensions.hostInstanceId);
    }
    if (customDimensions.azFunctionLiveLogsSessionId != null) {
      logRecord.setAttribute(
          AiSemanticAttributes.AZ_FN_LIVE_LOGS_SESSION_ID,
          customDimensions.azFunctionLiveLogsSessionId);
    }
  }
}
