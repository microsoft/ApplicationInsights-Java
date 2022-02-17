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
    attributeProcessor = AttributeProcessor.create(config);
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
    if (include != null && !include.isMatch(log.getAttributes(), log.getName(), true)) {
      // If not included we can skip further processing
      return log;
    }
    AgentProcessor.IncludeExclude exclude = attributeProcessor.getExclude();
    if (exclude != null && exclude.isMatch(log.getAttributes(), log.getName(), true)) {
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
