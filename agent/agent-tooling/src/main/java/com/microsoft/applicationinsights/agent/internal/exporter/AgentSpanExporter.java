// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMsgId.EXPORTER_MAPPING_ERROR;

import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.quickpulse.QuickPulse;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentSpanExporter implements SpanExporter {

  private static final Logger logger = LoggerFactory.getLogger(AgentSpanExporter.class);

  private static final OperationLogger exportingSpanLogger =
      new OperationLogger(SpanDataMapper.class, "Exporting span");

  private final SpanDataMapper mapper;
  private final Consumer<TelemetryItem> telemetryItemConsumer;

  public AgentSpanExporter(
      SpanDataMapper mapper,
      @Nullable QuickPulse quickPulse,
      BatchItemProcessor batchItemProcessor) {
    this.mapper = mapper;
    telemetryItemConsumer =
        telemetryItem -> {
          if (quickPulse != null) {
            quickPulse.add(telemetryItem);
          }
          TelemetryObservers.INSTANCE
              .getObservers()
              .forEach(consumer -> consumer.accept(telemetryItem));
          batchItemProcessor.trackAsync(telemetryItem);
        };
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (Strings.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      logger.debug("exporter is not active");
      // Azure Functions consumption plan
      return CompletableResultCode.ofSuccess();
    }
    for (SpanData span : spans) {
      logger.debug("exporting span: {}", span);
      try {
        mapper.map(span, telemetryItemConsumer);
        exportingSpanLogger.recordSuccess();
      } catch (Throwable t) {
        exportingSpanLogger.recordFailure(t.getMessage(), t, EXPORTER_MAPPING_ERROR);
      }
    }
    // always returning success, because all error handling is performed internally
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }
}
