// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class InheritedRoleNameLogProcessor implements LogProcessor {

  @Override
  public void onEmit(ReadWriteLogRecord logRecord) {
    Span currentSpan = Span.current();
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan currentReadableSpan = (ReadableSpan) currentSpan;
    String roleName = currentReadableSpan.getAttribute(AiSemanticAttributes.ROLE_NAME);
    if (roleName != null) {
      logRecord.setAttribute(AiSemanticAttributes.ROLE_NAME, roleName);
    }
  }
}
