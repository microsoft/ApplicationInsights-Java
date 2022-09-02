// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.processors.AgentProcessor.IncludeExclude;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExporterWithSpanProcessor implements SpanExporter {

  private final SpanExporter delegate;
  private final SpanProcessor spanProcessor;

  // caller should check config.isValid before creating
  public ExporterWithSpanProcessor(ProcessorConfig config, SpanExporter delegate) {
    config.validate();
    spanProcessor = SpanProcessor.create(config);
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    // we need to filter attributes before passing on to delegate
    List<SpanData> copy = new ArrayList<>();
    for (SpanData span : spans) {
      copy.add(process(span));
    }
    return delegate.export(copy);
  }

  private SpanData process(SpanData span) {
    IncludeExclude include = spanProcessor.getInclude();
    if (include != null && !include.isMatch(span.getAttributes(), span.getName())) {
      // If Not included we can skip further processing
      return span;
    }
    IncludeExclude exclude = spanProcessor.getExclude();
    if (exclude != null && exclude.isMatch(span.getAttributes(), span.getName())) {
      return span;
    }

    SpanData updatedSpan = spanProcessor.processFromAttributes(span);
    return spanProcessor.processToAttributes(updatedSpan);
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
