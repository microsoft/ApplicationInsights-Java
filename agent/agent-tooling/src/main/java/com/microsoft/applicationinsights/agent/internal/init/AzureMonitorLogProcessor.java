// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.OperationNames;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;

// See also AzureMonitorLogFilteringProcessor
public class AzureMonitorLogProcessor implements LogRecordProcessor {

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.fromContext(context);
    if (currentSpan instanceof ReadableSpan) {
      ReadableSpan readableSpan = (ReadableSpan) currentSpan;
      logRecord.setAttribute(
          AiSemanticAttributes.OPERATION_NAME, OperationNames.getOperationName(readableSpan));
    }
  }
}
