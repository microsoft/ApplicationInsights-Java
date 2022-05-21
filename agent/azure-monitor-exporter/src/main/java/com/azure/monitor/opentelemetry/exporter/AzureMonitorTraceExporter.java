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

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class is an implementation of OpenTelemetry {@link SpanExporter} that allows different
 * tracing services to export recorded data for sampled spans in their own format.
 */
// TODO (trask) move this class into internal package
public final class AzureMonitorTraceExporter implements SpanExporter {

  private static final ClientLogger LOGGER = new ClientLogger(AzureMonitorTraceExporter.class);

  private static final OperationLogger exportingSpanLogger =
      new OperationLogger(SpanDataMapper.class, "Exporting span");

  private final Function<List<TelemetryItem>, CompletableResultCode> exportFn;
  private final Supplier<CompletableResultCode> flushFn;
  private final Supplier<CompletableResultCode> shutdownFn;
  private final Supplier<Boolean> active;
  private final SpanDataMapper mapper;

  /**
   * Creates an instance of exporter that is configured with given exporter client that sends
   * telemetry events to Application Insights resource identified by the instrumentation key.
   */
  public AzureMonitorTraceExporter(
      Function<List<TelemetryItem>, CompletableResultCode> exportFn,
      Supplier<CompletableResultCode> flushFn,
      Supplier<CompletableResultCode> shutdownFn,
      Supplier<Boolean> active,
      SpanDataMapper mapper) {

    this.exportFn = exportFn;
    this.flushFn = flushFn;
    this.shutdownFn = shutdownFn;
    this.active = active;
    this.mapper = mapper;
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (!active.get()) {
      LOGGER.verbose("exporter is not active");
      return CompletableResultCode.ofSuccess();
    }

    List<TelemetryItem> telemetryItems = new ArrayList<>();

    boolean mappingFailure = false;
    for (SpanData span : spans) {
      LOGGER.verbose("exporting span: {}", span);
      try {
        mapper.map(span, telemetryItems);
        exportingSpanLogger.recordSuccess();
      } catch (Throwable t) {
        exportingSpanLogger.recordFailure(t.getMessage(), t);
        mappingFailure = true;
      }
    }

    if (telemetryItems.isEmpty()) {
      return mappingFailure ? CompletableResultCode.ofFailure() : CompletableResultCode.ofSuccess();
    }

    CompletableResultCode overallResult = new CompletableResultCode();
    CompletableResultCode exportResult = exportFn.apply(telemetryItems);
    boolean mappingFailureFinal = mappingFailure;
    exportResult.whenComplete(
        () -> {
          if (exportResult.isSuccess() && !mappingFailureFinal) {
            overallResult.succeed();
          } else {
            overallResult.fail();
          }
        });

    return overallResult;
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode flush() {
    return flushFn.get();
  }

  /** {@inheritDoc} */
  @Override
  public CompletableResultCode shutdown() {
    return shutdownFn.get();
  }
}
