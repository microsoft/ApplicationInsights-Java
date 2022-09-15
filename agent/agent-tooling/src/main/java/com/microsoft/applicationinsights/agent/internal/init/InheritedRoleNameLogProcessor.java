// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;

public class InheritedRoleNameLogProcessor implements LogProcessor {

  @Override
  public void onEmit(ReadWriteLogRecord logRecord) {
    Context context = Context.current();
    String connectionString = context.get(AiContextKeys.ROLE_NAME);
    if (connectionString != null) {
      logRecord.setAttribute(AiSemanticAttributes.INTERNAL_ROLE_NAME, connectionString);
    }
  }
}
