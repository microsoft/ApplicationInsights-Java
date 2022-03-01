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
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulseDataCollector;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class LiveMetricsSpanProcessor implements SpanProcessor {

  private static final ClientLogger LOGGER = new ClientLogger(AzureMonitorTraceExporter.class);

  private final String instrumentationKey;

  public LiveMetricsSpanProcessor(
      HttpPipeline httpPipeline,
      String instrumentationKey,
      @Nullable String roleName,
      @Nullable String roleInstance,
      String endpointUrl) {
    this.instrumentationKey = instrumentationKey;
    QuickPulse.INSTANCE.initialize(
        httpPipeline, () -> instrumentationKey, roleName, roleInstance, endpointUrl);
  }

  @Override
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {
    SpanData spanData = readWriteSpan.toSpanData();
    SpanKind kind = spanData.getKind();
    String instrumentationName = spanData.getInstrumentationLibraryInfo().getName();

    List<TelemetryItem> telemetryItems = new ArrayList<>();
    if (kind == SpanKind.INTERNAL) {
      if (instrumentationName.startsWith("io.opentelemetry.spring-scheduling-")
          && !spanData.getParentSpanContext().isValid()) {
        // if (!span.getParentSpanContext().isValid()) {
        // TODO (trask) AI mapping: need semantic convention for determining whether to map INTERNAL
        // to request or dependency (or need clarification to use SERVER for this)
        TelemetryMappingHelper.addRequestTelemetryItem(
            spanData, instrumentationKey, telemetryItems);
      } else {
        TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
            spanData, true, instrumentationKey, telemetryItems);
      }
    } else if (kind == SpanKind.CLIENT || kind == SpanKind.PRODUCER) {
      TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
          spanData, false, instrumentationKey, telemetryItems);
    } else if (kind == SpanKind.CONSUMER
        && "receive".equals(spanData.getAttributes().get(SemanticAttributes.MESSAGING_OPERATION))) {
      TelemetryMappingHelper.addRemoteDependencyTelemetryItem(
          spanData, false, instrumentationKey, telemetryItems);
    } else if (kind == SpanKind.SERVER || kind == SpanKind.CONSUMER) {
      TelemetryMappingHelper.addRequestTelemetryItem(spanData, instrumentationKey, telemetryItems);
    } else {
      throw LOGGER.logExceptionAsError(new UnsupportedOperationException(kind.name()));
    }

    for (TelemetryItem telemetryItem : telemetryItems) {
      QuickPulseDataCollector.INSTANCE.add(telemetryItem);
    }
  }

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
