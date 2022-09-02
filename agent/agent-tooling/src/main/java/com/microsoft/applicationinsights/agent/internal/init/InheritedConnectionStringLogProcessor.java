// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class InheritedConnectionStringLogProcessor implements LogProcessor {

  @Override
  public void onEmit(ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.current();
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan currentReadableSpan = (ReadableSpan) currentSpan;
    String connectionString =
        currentReadableSpan.getAttribute(AiSemanticAttributes.CONNECTION_STRING);
    if (connectionString != null) {
      logRecord.setAttribute(AiSemanticAttributes.CONNECTION_STRING, connectionString);
    } else {
      // back compat support
      String instrumentationKey =
          currentReadableSpan.getAttribute(AiSemanticAttributes.INSTRUMENTATION_KEY);
      if (instrumentationKey != null) {
        logRecord.setAttribute(AiSemanticAttributes.INSTRUMENTATION_KEY, instrumentationKey);
      }
    }
  }
}
