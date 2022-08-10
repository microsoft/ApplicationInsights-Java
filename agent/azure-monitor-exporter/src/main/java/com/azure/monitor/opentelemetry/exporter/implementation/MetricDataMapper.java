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

package com.azure.monitor.opentelemetry.exporter.implementation;

import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.RequestCustomDimensionsExtractor.updatePreAggMetricsCustomDimensions;
import static io.opentelemetry.api.internal.Utils.checkArgument;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.HISTOGRAM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricPointBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricDataMapper {

  private static final List<String> OTEL_PRE_AGGREGATED_METRIC_NAMES = new ArrayList<>();
  private static final List<String> EXCLUDED_METRIC_NAMES = new ArrayList<>();

  private static final Logger logger = LoggerFactory.getLogger(MetricDataMapper.class);
  private final BiConsumer<AbstractTelemetryBuilder, Resource> telemetryInitializer;
  private final boolean captureHttpServer4xxAsError;

  static {
    EXCLUDED_METRIC_NAMES.add("http.server.active_requests"); // Servlet

    OTEL_PRE_AGGREGATED_METRIC_NAMES.add("http.server.duration"); // Servlet
    OTEL_PRE_AGGREGATED_METRIC_NAMES.add("http.client.duration"); // HttpClient
    OTEL_PRE_AGGREGATED_METRIC_NAMES.add("rpc.client.duration"); // gRPC
    OTEL_PRE_AGGREGATED_METRIC_NAMES.add("rpc.server.duration"); // gRPC
  }

  public MetricDataMapper(
      BiConsumer<AbstractTelemetryBuilder, Resource> telemetryInitializer,
      boolean captureHttpServer4xxAsError) {
    this.telemetryInitializer = telemetryInitializer;
    this.captureHttpServer4xxAsError = captureHttpServer4xxAsError;
  }

  public void map(MetricData metricData, Consumer<TelemetryItem> consumer) {
    if (EXCLUDED_METRIC_NAMES.contains(metricData.getName())) {
      return;
    }

    MetricDataType type = metricData.getType();
    if (type == DOUBLE_SUM
        || type == DOUBLE_GAUGE
        || type == LONG_SUM
        || type == LONG_GAUGE
        || type == HISTOGRAM) {
      boolean isPreAggregated = OTEL_PRE_AGGREGATED_METRIC_NAMES.contains(metricData.getName());
      List<TelemetryItem> telemetryItemList =
          convertOtelMetricToAzureMonitorMetric(metricData, isPreAggregated);
      for (TelemetryItem telemetryItem : telemetryItemList) {
        consumer.accept(telemetryItem);
      }
    } else {
      logger.warn("metric data type {} is not supported yet.", metricData.getType());
    }
  }

  private List<TelemetryItem> convertOtelMetricToAzureMonitorMetric(
      MetricData metricData, boolean isPreAggregated) {
    List<TelemetryItem> telemetryItems = new ArrayList<>();

    for (PointData pointData : metricData.getData().getPoints()) {
      MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
      telemetryInitializer.accept(builder, metricData.getResource());

      builder.setTime(FormattedTime.offSetDateTimeFromEpochNanos(pointData.getEpochNanos()));
      updateMetricPointBuilder(
          builder, metricData, pointData, captureHttpServer4xxAsError, isPreAggregated);

      telemetryItems.add(builder.build());
    }
    return telemetryItems;
  }

  // visible for testing
  public static void updateMetricPointBuilder(
      MetricTelemetryBuilder metricTelemetryBuilder,
      MetricData metricData,
      PointData pointData,
      boolean captureHttpServer4xxAsError,
      boolean isPreAggregated) {
    checkArgument(metricData != null, "MetricData cannot be null.");

    MetricPointBuilder pointBuilder = new MetricPointBuilder();
    MetricDataType type = metricData.getType();
    double pointDataValue;
    switch (type) {
      case LONG_SUM:
      case LONG_GAUGE:
        pointDataValue = (double) ((LongPointData) pointData).getValue();
        break;
      case DOUBLE_SUM:
      case DOUBLE_GAUGE:
        pointDataValue = ((DoublePointData) pointData).getValue();
        break;
      case HISTOGRAM:
        long histogramCount = ((HistogramPointData) pointData).getCount();
        if (histogramCount <= Integer.MAX_VALUE && histogramCount >= Integer.MIN_VALUE) {
          pointBuilder.setCount((int) histogramCount);
        }
        HistogramPointData histogramPointData = (HistogramPointData) pointData;
        pointDataValue = histogramPointData.getSum();
        pointBuilder.setMin(histogramPointData.getMin());
        pointBuilder.setMax(histogramPointData.getMax());
        break;
      case SUMMARY: // not supported yet in OpenTelemetry SDK
      case EXPONENTIAL_HISTOGRAM: // not supported yet in OpenTelemetry SDK
      default:
        throw new IllegalArgumentException("metric data type '" + type + "' is not supported yet");
    }

    pointBuilder.setValue(pointDataValue);
    pointBuilder.setName(metricData.getName());
    metricTelemetryBuilder.setMetricPoint(pointBuilder);

    if (isPreAggregated) {
      // TODO update value if applicable
      Long statusCode = pointData.getAttributes().get(AttributeKey.longKey("http.status_code"));
      updatePreAggMetricsCustomDimensions(
          metricTelemetryBuilder,
          pointDataValue,
          statusCode,
          getSuccess(statusCode, captureHttpServer4xxAsError));
    } else {
      pointData
          .getAttributes()
          .forEach(
              (key, value) -> metricTelemetryBuilder.addProperty(key.getKey(), value.toString()));
    }
  }

  private static boolean getSuccess(Long statusCode, boolean captureHttpServer4xxAsError) {
    if (captureHttpServer4xxAsError) {
      return statusCode == null || statusCode < 400;
    }

    return statusCode == 200;
  }
}
