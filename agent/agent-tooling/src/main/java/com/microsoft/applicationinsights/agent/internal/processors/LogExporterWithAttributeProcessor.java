// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.export.LogExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LogExporterWithAttributeProcessor implements LogExporter {

  public final LogExporter delegate;
  private final AttributeProcessor attributeProcessor;

  // caller should check config.isValid before creating
  public LogExporterWithAttributeProcessor(
      Configuration.ProcessorConfig config, LogExporter delegate) {
    config.validate();
    attributeProcessor = AttributeProcessor.create(config, true);
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<LogData> logs) {
    // we need to filter attributes before passing on to delegate
    List<LogData> copy = new ArrayList<>();
    for (LogData log : logs) {
      copy.add(process(log));
    }
    return delegate.export(copy);
  }

  private LogData process(LogData log) {
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
