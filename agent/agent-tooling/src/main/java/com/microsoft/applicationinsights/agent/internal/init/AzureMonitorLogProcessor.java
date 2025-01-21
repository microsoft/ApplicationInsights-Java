// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.OperationNames;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.SemanticAttributes;
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
  private static final Field lockField;
  private static final Field attributesMapField;

  static {
    Class<?> sdkReadWriteLogRecordClass = getSdkReadWriteLogRecordClass();
    lockField = getLockField(sdkReadWriteLogRecordClass);
    attributesMapField = getAttributesMapField(sdkReadWriteLogRecordClass);
  }

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
    Double sampleRate = readableSpan.getAttribute(AiSemanticAttributes.SAMPLE_RATE);
    if (sampleRate != null) {
      logRecord.setAttribute(AiSemanticAttributes.SAMPLE_RATE, sampleRate);
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
  private static Field getLockField(Class<?> sdkReadWriteLogRecordClass) {
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
  private static Field getAttributesMapField(Class<?> sdkReadWriteLogRecordClass) {
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
        // TODO add `getAttribute()` to `ReadWriteLogRecord` upstream
        stacktrace =
            ((AttributesMap) attributesMapField.get(logRecord))
                .get(SemanticAttributes.EXCEPTION_STACKTRACE);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    if (stacktrace != null) {
      span.setAttribute(AiSemanticAttributes.LOGGED_EXCEPTION, stacktrace);
    }
  }
}
