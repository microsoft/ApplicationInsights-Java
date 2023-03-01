// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMsgId.EXPORTER_MAPPING_ERROR;

import com.azure.monitor.opentelemetry.exporter.implementation.MetricDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.MetricFilter;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentMetricExporter implements MetricExporter {

  private static final Logger logger = LoggerFactory.getLogger(AgentMetricExporter.class);

  private static final OperationLogger exportingMetricLogger =
      new OperationLogger(AgentMetricExporter.class, "Exporting metric");

  private final List<MetricFilter> metricFilters;
  private final MetricDataMapper mapper;
  private final Consumer<TelemetryItem> telemetryItemConsumer;

  public AgentMetricExporter(
      List<MetricFilter> metricFilters,
      MetricDataMapper mapper,
      BatchItemProcessor batchItemProcessor) {
    this.metricFilters = metricFilters;
    this.mapper = mapper;
    this.telemetryItemConsumer =
        telemetryItem -> {
          TelemetryObservers.INSTANCE
              .getObservers()
              .forEach(consumer -> consumer.accept(telemetryItem));
          batchItemProcessor.trackAsync(telemetryItem);
        };
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    if (Strings.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      // Azure Functions consumption plan
      logger.debug("exporter is not active");
      return CompletableResultCode.ofSuccess();
    }
    for (MetricData metricData : metrics) {
      if (MetricFilter.shouldSkip(metricData.getName(), metricFilters)) {
        continue;
      }
      logger.debug("exporting metric: {}", metricData);
      try {
        mapper.map(metricData, telemetryItemConsumer);
        exportingMetricLogger.recordSuccess();
      } catch (Throwable t) {
        exportingMetricLogger.recordFailure(t.getMessage(), t, EXPORTER_MAPPING_ERROR);
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

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return AggregationTemporalitySelector.deltaPreferred()
        .getAggregationTemporality(instrumentType);
  }
}
