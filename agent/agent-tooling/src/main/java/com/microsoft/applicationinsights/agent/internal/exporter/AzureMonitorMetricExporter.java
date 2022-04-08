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

package com.microsoft.applicationinsights.agent.internal.exporter;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.HISTOGRAM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricPointBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureMonitorMetricExporter implements MetricExporter {

  private static final String DEFAULT_METRIC_AGGREGATION_INTERVAL = "60000";
  private static final String AGGREGATION_INTERNAL_MS_KEY = "_MS.AggregationIntervalMs";
  private static final List<String> EXCLUDED_METRIC_NAMES = new ArrayList<>();

  private final TelemetryClient telemetryClient;
  private static final Logger logger = LoggerFactory.getLogger(AzureMonitorMetricExporter.class);
  private final AtomicBoolean stopped = new AtomicBoolean();

  public AzureMonitorMetricExporter(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  static {
    EXCLUDED_METRIC_NAMES.add("http.server.active_requests"); // Micrometer
    EXCLUDED_METRIC_NAMES.add("http.server.duration"); // Micrometer
    EXCLUDED_METRIC_NAMES.add("http.client.duration"); // HttpClient
    EXCLUDED_METRIC_NAMES.add("rpc.client.duration"); // gRPC
    EXCLUDED_METRIC_NAMES.add("rpc.server.duration"); // gRPC
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    if (stopped.get()) {
      return CompletableResultCode.ofFailure();
    }

    for (MetricData metricData : metrics) {
      if (EXCLUDED_METRIC_NAMES.contains(metricData.getName())) {
        continue;
      }

      MetricDataType type = metricData.getType();
      if (type == DOUBLE_SUM
          || type == DOUBLE_GAUGE
          || type == LONG_SUM
          || type == LONG_GAUGE
          || type == HISTOGRAM) {
        convertOtelMetricToAzureMonitorMetric(metricData).forEach(telemetryClient::trackAsync);
      } else {
        logger.warn("metric data type {} is not supported yet.", type);
      }
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    stopped.set(true);
    return CompletableResultCode.ofSuccess();
  }

  private List<TelemetryItem> convertOtelMetricToAzureMonitorMetric(MetricData metricData) {
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    for (PointData data : metricData.getData().getPoints()) {
      MetricTelemetryBuilder builder = telemetryClient.newMetricTelemetryBuilder();
      builder.setTime(FormattedTime.offSetDateTimeFromEpochNanos(data.getStartEpochNanos()));
      updateMetricPointBuilder(builder, metricData, data);
      telemetryItems.add(builder.build());
    }
    return telemetryItems;
  }

  // visible for testing
  static void updateMetricPointBuilder(
      MetricTelemetryBuilder metricTelemetryBuilder, MetricData metricData, PointData pointData) {
    checkArgument(metricData != null, "MetricData cannot be null.");

    MetricPointBuilder pointBuilder = new MetricPointBuilder();
    MetricDataType type = metricData.getType();
    switch (type) {
      case LONG_SUM:
        pointBuilder.setDataPointType(DataPointType.AGGREGATION);
        pointBuilder.setValue(((LongPointData) pointData).getValue());
        break;
      case LONG_GAUGE:
        pointBuilder.setDataPointType(DataPointType.MEASUREMENT);
        pointBuilder.setValue(((LongPointData) pointData).getValue());
        break;
      case DOUBLE_SUM:
        pointBuilder.setDataPointType(DataPointType.AGGREGATION);
        pointBuilder.setValue(((DoublePointData) pointData).getValue());
        break;
      case DOUBLE_GAUGE:
        pointBuilder.setDataPointType(DataPointType.MEASUREMENT);
        pointBuilder.setValue(((DoublePointData) pointData).getValue());
        break;
      case HISTOGRAM:
        pointBuilder.setDataPointType(DataPointType.AGGREGATION);
        long histogramCount = ((HistogramPointData) pointData).getCount();
        if (histogramCount <= Integer.MAX_VALUE && histogramCount >= Integer.MIN_VALUE) {
          pointBuilder.setCount((int) histogramCount);
        }
        pointBuilder.setValue(((HistogramPointData) pointData).getSum());
        // TODO track min/max when it becomes available
        break;
      case SUMMARY: // not supported yet in OpenTelemetry SDK
      case EXPONENTIAL_HISTOGRAM: // not supported yet in OpenTelemetry SDK
      default:
        throw new IllegalArgumentException("metric data type '" + type + "' is not supported yet");
    }

    pointBuilder.setName(metricData.getName());

    metricTelemetryBuilder.setMetricPoint(pointBuilder);

    pointData
        .getAttributes()
        .forEach(
            (key, value) -> metricTelemetryBuilder.addProperty(key.getKey(), value.toString()));
    metricTelemetryBuilder.addProperty(
        AGGREGATION_INTERNAL_MS_KEY, DEFAULT_METRIC_AGGREGATION_INTERVAL);
  }
}
