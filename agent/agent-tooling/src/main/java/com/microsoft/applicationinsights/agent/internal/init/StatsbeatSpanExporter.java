// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.statsbeat.StatsbeatModule;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;

import static com.azure.monitor.opentelemetry.exporter.implementation.statsbeat.Instrumentations.AZURE_OPENTELEMETRY;

public class StatsbeatSpanExporter implements SpanExporter {

  private final SpanExporter delegate;
  private final StatsbeatModule statsbeatModule;

  public StatsbeatSpanExporter(SpanExporter delegate, StatsbeatModule statsbeatModule) {
    this.delegate = delegate;
    this.statsbeatModule = statsbeatModule;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    for (SpanData span : spans) {
      String instrumentationScopeName = span.getInstrumentationScopeInfo().getName();
      if (instrumentationScopeName.startsWith("com.azure")) {
        instrumentationScopeName = AZURE_OPENTELEMETRY;
      }
      statsbeatModule
          .getInstrumentationStatsbeat()
          .addInstrumentation(instrumentationScopeName);
    }
    return delegate.export(spans);
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
