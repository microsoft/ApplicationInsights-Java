// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.processors.AgentProcessor.IncludeExclude;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.export.LogExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExporterWithLogProcessor implements LogExporter {

  private final LogExporter delegate;
  private final LogProcessor logProcessor;

  // caller should check config.isValid before creating
  public ExporterWithLogProcessor(ProcessorConfig config, LogExporter delegate) {
    config.validate();
    logProcessor = LogProcessor.create(config);
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
    IncludeExclude include = logProcessor.getInclude();
    if (include != null && !include.isMatch(log.getAttributes(), log.getBody().asString())) {
      // If Not included we can skip further processing
      return log;
    }
    IncludeExclude exclude = logProcessor.getExclude();
    if (exclude != null && exclude.isMatch(log.getAttributes(), log.getBody().asString())) {
      return log;
    }

    LogData updatedLog = logProcessor.processFromAttributes(log);
    return logProcessor.processToAttributes(updatedLog);
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
