// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.OperationNames;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureMonitorLogProcessor implements LogRecordProcessor {

  private static final Logger logger = LoggerFactory.getLogger("duplicate.logging.detector");

  private volatile String priorMessage;
  private volatile StackTraceElement[] priorStackTraceElements;

  private static String toString(StackTraceElement[] stackTraceElements) {
    StringBuilder sb = new StringBuilder();
    for (StackTraceElement stackTraceElement : stackTraceElements) {
      sb.append("\n");
      sb.append(stackTraceElement);
    }
    return sb.toString();
  }

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {

    Body body = logRecord.toLogRecordData().getBody();
    String message = body.asString();
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    if (message.contains("executionId") && message.equals(priorMessage)) {
      logger.warn(
          "FOUND A POSSIBLE DUPLICATE: "
              + message
              + "\nPRIOR STACK TRACE:"
              + toString(priorStackTraceElements)
              + "\nCURRENT STACK TRACE:"
              + toString(stackTraceElements));
    }

    priorMessage = message;
    priorStackTraceElements = stackTraceElements;

    Span currentSpan = Span.fromContext(context);
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
