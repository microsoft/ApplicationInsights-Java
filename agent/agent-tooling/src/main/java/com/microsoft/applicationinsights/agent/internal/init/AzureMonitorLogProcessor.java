// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.OperationNames;
import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctionsCustomDimensions;
import com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class AzureMonitorLogProcessor implements LogRecordProcessor {

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.fromContext(context);
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) currentSpan;
    if (ConfigurationBuilder.inAzureFunctionsWorker()) {
      AzureFunctionsCustomDimensions customDimensions =
          AzureFunctionsCustomDimensions.fromContext(context);
      if (customDimensions != null) {
        logRecord.setAttribute(AiSemanticAttributes.OPERATION_NAME, customDimensions.operationName);
      }
    } else {
      logRecord.setAttribute(
          AiSemanticAttributes.OPERATION_NAME, OperationNames.getOperationName(readableSpan));
    }
    Long itemCount = readableSpan.getAttribute(AiSemanticAttributes.ITEM_COUNT);
    if (itemCount != null) {
      logRecord.setAttribute(AiSemanticAttributes.ITEM_COUNT, itemCount);
    }
  }
}
