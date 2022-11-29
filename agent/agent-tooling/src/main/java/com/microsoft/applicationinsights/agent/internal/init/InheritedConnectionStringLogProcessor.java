// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;

public final class InheritedConnectionStringLogProcessor implements LogRecordProcessor {

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    String connectionString = context.get(AiContextKeys.CONNECTION_STRING);
    if (connectionString != null) {
      logRecord.setAttribute(AiSemanticAttributes.INTERNAL_CONNECTION_STRING, connectionString);
    }
  }
}
