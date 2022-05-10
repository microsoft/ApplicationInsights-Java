/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
