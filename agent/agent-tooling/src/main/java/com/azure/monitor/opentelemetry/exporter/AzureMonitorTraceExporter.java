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

package com.azure.monitor.opentelemetry.exporter;

import com.azure.core.http.HttpPipeline;
import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is an implementation of OpenTelemetry {@link SpanExporter} that allows different
 * tracing services to export recorded data for sampled spans in their own format.
 */
public final class AzureMonitorTraceExporter implements SpanExporter {

  private static final ClientLogger LOGGER = new ClientLogger(AzureMonitorTraceExporter.class);
  private final TelemetryItemExporter telemetryItemExporter;
  private final String instrumentationKey;

  /**
   * Creates an instance of exporter that is configured with given exporter client that sends
   * telemetry events to Application Insights resource identified by the instrumentation key.
   *
   * @param httpPipeline The http pipeline used to send data to Azure Monitor.
   * @param endpoint the ingestion endpoint used to send data to Azure Monitor.
   * @param instrumentationKey The instrumentation key of Application Insights resource.
   */
  AzureMonitorTraceExporter(HttpPipeline httpPipeline, URL endpoint, String instrumentationKey) {
    this.telemetryItemExporter =
        new TelemetryItemExporter(
            new TelemetryPipeline(httpPipeline, endpoint), TelemetryPipelineListener.noop());
    this.instrumentationKey = instrumentationKey;
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    CompletableResultCode completableResultCode = new CompletableResultCode();
    try {
      List<TelemetryItem> telemetryItems = new ArrayList<>();
      for (SpanData span : spans) {
        LOGGER.verbose("exporting span: {}", span);
        exportInternal(span, telemetryItems);
      }
      return telemetryItemExporter.send(telemetryItems);
    } catch (Throwable t) {
      LOGGER.error(t.getMessage(), t);
      return completableResultCode.fail();
    }
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  private void exportInternal(SpanData span, List<TelemetryItem> telemetryItems) {
    SpanKind kind = span.getKind();
    String instrumentationName = span.getInstrumentationLibraryInfo().getName();
    if (kind == SpanKind.INTERNAL) {
      if (instrumentationName.startsWith("io.opentelemetry.spring-scheduling-")
          && !span.getParentSpanContext().isValid()) {
        // if (!span.getParentSpanContext().isValid()) {
        // TODO (trask) AI mapping: need semantic convention for determining whether to map INTERNAL
        // to request or dependency (or need clarification to use SERVER for this)
        TelemetryMappingHelper.addRequestTelemetryItem(span, instrumentationKey, telemetryItems);
      } else {
        TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
            span, true, instrumentationKey, telemetryItems);
      }
    } else if (kind == SpanKind.CLIENT || kind == SpanKind.PRODUCER) {
      TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
          span, false, instrumentationKey, telemetryItems);
    } else if (kind == SpanKind.CONSUMER
        && "receive".equals(span.getAttributes().get(SemanticAttributes.MESSAGING_OPERATION))) {
      TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
          span, false, instrumentationKey, telemetryItems);
    } else if (kind == SpanKind.SERVER || kind == SpanKind.CONSUMER) {
      TelemetryMappingHelper.addRequestTelemetryItem(span, instrumentationKey, telemetryItems);
    } else {
      throw LOGGER.logExceptionAsError(new UnsupportedOperationException(kind.name()));
    }
  }
}
