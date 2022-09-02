// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.OperationNames;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class AzureMonitorLogProcessor implements LogProcessor {

  @Override
  public void onEmit(ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.current();
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) currentSpan;

    logRecord.setAttribute(
        AiSemanticAttributes.OPERATION_NAME, OperationNames.getOperationName(readableSpan));
    Long itemCount = readableSpan.getAttribute(AiSemanticAttributes.ITEM_COUNT);
    if (itemCount != null) {
      logRecord.setAttribute(AiSemanticAttributes.ITEM_COUNT, itemCount);
    }
  }
}
