// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.OperationNames;
import com.azure.monitor.opentelemetry.exporter.implementation.SemanticAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.internal.AttributesMap;
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
    setAttributeExceptionLogged(LocalRootSpan.fromContext(context), logRecord);

    ReadableSpan readableSpan = (ReadableSpan) currentSpan;
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

  private static void setAttributeExceptionLogged(Span span, ReadWriteLogRecord logRecord) {
    if (lockField == null || attributesMapField == null) {
      return;
    }
    String stacktrace = null;
    try {
      synchronized (lockField) {
        stacktrace =
            ((AttributesMap) attributesMapField.get(logRecord))
                .get(SemanticAttributes.EXCEPTION_STACKTRACE);
      }
    } catch (Exception e) {
      logger.error(
          "Error occurred while stamping \"applicationinsights.internal.exception_logged\" to the span.",
          e);
    }
    if (stacktrace != null) {
      span.setAttribute(
          "applicationinsights.internal.exception_logged",
          stacktrace); // TODO (heya) this should be AiSemanticAttributes.EXCEPTION_LOGGED
      System.out.println(
          "add \"applicationinsights.internal.exception_logged\" attribute to the span.");
    }
  }
}
