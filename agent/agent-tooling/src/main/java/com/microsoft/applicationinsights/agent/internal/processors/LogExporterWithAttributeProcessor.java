// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LogExporterWithAttributeProcessor implements LogRecordExporter {

  public final LogRecordExporter delegate;
  private final AttributeProcessor attributeProcessor;

  // caller should check config.isValid before creating
  public LogExporterWithAttributeProcessor(
      Configuration.ProcessorConfig config, LogRecordExporter delegate) {
    config.validate();
    attributeProcessor = AttributeProcessor.create(config, true);
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    // we need to filter attributes before passing on to delegate
    List<LogRecordData> copy = new ArrayList<>();
    for (LogRecordData log : logs) {
      copy.add(process(log));
    }
    return delegate.export(copy);
  }

  private LogRecordData process(LogRecordData log) {
    AgentProcessor.IncludeExclude include = attributeProcessor.getInclude();
    if (include != null && !include.isMatch(log.getAttributes(), log.getBody().asString())) {
      // If not included we can skip further processing
      return log;
    }
    AgentProcessor.IncludeExclude exclude = attributeProcessor.getExclude();
    if (exclude != null && exclude.isMatch(log.getAttributes(), log.getBody().asString())) {
      // If excluded we can skip further processing
      return log;
    }

    return attributeProcessor.processActions(log);
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }
}
