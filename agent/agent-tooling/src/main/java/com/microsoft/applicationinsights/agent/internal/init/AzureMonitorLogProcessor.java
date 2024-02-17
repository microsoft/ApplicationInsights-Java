// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.OperationNames;
import com.azure.monitor.opentelemetry.exporter.implementation.SemanticAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.lang.reflect.Field;
import javax.annotation.Nullable;

public class AzureMonitorLogProcessor implements LogRecordProcessor {

  private static final ClientLogger logger = new ClientLogger(AzureMonitorLogProcessor.class);

  private static final Class<?> sdkReadWriteLogRecordClass = getSdkReadWriteLogRecordClass();
  private static final Field lockField = getLockField();
  private static final Field attributesMapField = getAttributesMapField();

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.fromContext(context);
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan readableSpan = (ReadableSpan) currentSpan;
    String stacktrace = readableSpan.getAttribute(SemanticAttributes.EXCEPTION_STACKTRACE);
    if (stacktrace != null) {
      setExceptionAlreadyLogged(stacktrace, currentSpan);
    }

    logRecord.setAttribute(
        AiSemanticAttributes.OPERATION_NAME, OperationNames.getOperationName(readableSpan));
    Long itemCount = readableSpan.getAttribute(AiSemanticAttributes.ITEM_COUNT);
    if (itemCount != null) {
      logRecord.setAttribute(AiSemanticAttributes.ITEM_COUNT, itemCount);
    }
  }

  @Nullable
  private static Class<?> getSdkReadWriteLogRecordClass() {
    try {
      return Class.forName("io.opentelemetry.sdk.logs.SdkReadWriteLogRecord");
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Nullable
  private static Field getLockField() {
    if (sdkReadWriteLogRecordClass == null) {
      return null;
    }
    try {
      Field lockField = sdkReadWriteLogRecordClass.getDeclaredField("lock");
      lockField.setAccessible(true);
      return lockField;
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  @Nullable
  private static Field getAttributesMapField() {
    if (sdkReadWriteLogRecordClass == null) {
      return null;
    }
    try {
      Field attributesMapField = sdkReadWriteLogRecordClass.getDeclaredField("attributes");
      attributesMapField.setAccessible(true);
      return attributesMapField;
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  private static void setExceptionAlreadyLogged(String stacktraceFromSpan, Span span) {
    try {
      if (lockField != null && attributesMapField != null) {
        synchronized (lockField) {
          String stacktrace =
              (String) attributesMapField.get(SemanticAttributes.EXCEPTION_STACKTRACE);
          if (stacktrace != null && stacktrace.equals(stacktraceFromSpan)) {
            span.setAttribute("applicationinsights.internal.exception_already_logged", true);
            logger.verbose(
                "add \"applicationinsights.internal.exception_already_logged\" attribute to the span.");
          }
        }
      }
    } catch (Exception e) {
      logger.error(
          "Error occurred while stamping \"applicationinsights.internal.exception_already_logged\" to the span.",
          e);
    }
  }
}
