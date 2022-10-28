// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.monitor.opentelemetry.exporter.implementation.livemetrics;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.SemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;

public class LiveMetricsLogProcessor implements LogRecordProcessor {

  private final LogDataMapper mapper;
  private final QuickPulse quickPulse;

  public LiveMetricsLogProcessor(LogDataMapper mapper, QuickPulse quickPulse) {
    this.mapper = mapper;
    this.quickPulse = quickPulse;
  }

  @Override
  public void onEmit(ReadWriteLogRecord logRecord) {
    LogRecordData log = (LogRecordData) logRecord;
    String stack = log.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);
    Long itemCount = log.getAttributes().get(AiSemanticAttributes.ITEM_COUNT);
    if (quickPulse.isEnabled()) {
      quickPulse.add(mapper.map(logRecord.toLogRecordData(), stack, itemCount));
    }
  }
}
