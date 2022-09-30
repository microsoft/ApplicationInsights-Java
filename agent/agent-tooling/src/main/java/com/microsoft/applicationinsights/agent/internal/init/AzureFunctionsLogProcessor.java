// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctionsCustomDimensions;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;

public class AzureFunctionsLogProcessor implements LogProcessor {

  private static final ClientLogger logger = new ClientLogger(AzureFunctionsLogProcessor.class);

  @Override
  public void onEmit(ReadWriteLogRecord logRecord) {
    AzureFunctionsCustomDimensions customDimensions =
        AzureFunctionsCustomDimensions.fromContext(Context.current());
    logger.verbose(
        "####### AzureFunctionsLogProcessor::onEmit:: \n CustomDimensions: {}",
        customDimensions.toString());
    if (customDimensions != null) {
      logRecord.setAttribute(AiSemanticAttributes.INVOCATION_ID, customDimensions.invocationId);
      logRecord.setAttribute(AiSemanticAttributes.PROCESS_ID, customDimensions.processId);
      logRecord.setAttribute(AiSemanticAttributes.LOG_LEVEL, customDimensions.logLevel);
      logRecord.setAttribute(AiSemanticAttributes.CATEGORY, customDimensions.category);
      logRecord.setAttribute(
          AiSemanticAttributes.HOST_INSTANCE_ID, customDimensions.hostInstanceId);
      logRecord.setAttribute(
          AiSemanticAttributes.AZ_FUNC_LIVE_LOGS_SESSION_ID,
          customDimensions.azFunctionLiveLogsSessionId);
    }
  }
}
