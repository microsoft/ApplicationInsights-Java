// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MockLoggerExporter implements LogRecordExporter {

  private final List<LogRecordData> logs = new ArrayList<>();

  public List<LogRecordData> getLogs() {
    return logs;
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    this.logs.addAll(logs);
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }
}
