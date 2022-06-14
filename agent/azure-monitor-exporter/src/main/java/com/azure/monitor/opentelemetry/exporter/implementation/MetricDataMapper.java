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

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.HISTOGRAM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricPointBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.ResourceParser;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.VersionGenerator;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricDataMapper {

  private static final List<String> EXCLUDED_METRIC_NAMES = new ArrayList<>();

  private static final Logger logger = LoggerFactory.getLogger(MetricDataMapper.class);
  private final String instrumentationKey;
  private final Consumer<AbstractTelemetryBuilder> telemetryInitializer;

  static {
    EXCLUDED_METRIC_NAMES.add("http.server.active_requests"); // Servlet
    EXCLUDED_METRIC_NAMES.add("http.server.duration"); // Servlet
    EXCLUDED_METRIC_NAMES.add("http.client.duration"); // HttpClient
    EXCLUDED_METRIC_NAMES.add("rpc.client.duration"); // gRPC
    EXCLUDED_METRIC_NAMES.add("rpc.server.duration"); // gRPC
  }

  public MetricDataMapper(
      String instrumentationKey, Consumer<AbstractTelemetryBuilder> telemetryInitializer) {
    this.instrumentationKey = instrumentationKey;
    this.telemetryInitializer = telemetryInitializer;
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
      List<TelemetryItem> telemetryItemList = convertOtelMetricToAzureMonitorMetric(metricData);
      for (TelemetryItem telemetryItem : telemetryItemList) {
        consumer.accept(telemetryItem);
      }
    } else {
      logger.warn("metric data type {} is not supported yet.", metricData.getType());
    }
  }

  private List<TelemetryItem> convertOtelMetricToAzureMonitorMetric(MetricData metricData) {
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    for (PointData pointData : metricData.getData().getPoints()) {
      MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
      telemetryInitializer.accept(builder);
      builder.setInstrumentationKey(instrumentationKey);
      builder.addTag(
          ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString(), VersionGenerator.getSdkVersion());
      builder.setTime(FormattedTime.offSetDateTimeFromEpochNanos(pointData.getEpochNanos()));
      updateMetricPointBuilder(builder, metricData, pointData);
      ResourceParser.updateRoleNameAndInstance(builder, metricData.getResource());
      telemetryItems.add(builder.build());
    }
    return telemetryItems;
  }

  // visible for testing
  public static void updateMetricPointBuilder(
      MetricTelemetryBuilder metricTelemetryBuilder, MetricData metricData, PointData pointData) {
    checkArgument(metricData != null, "MetricData cannot be null.");

    MetricPointBuilder pointBuilder = new MetricPointBuilder();
    MetricDataType type = metricData.getType();
    switch (type) {
      case LONG_SUM:
        pointBuilder.setValue((double) ((LongPointData) pointData).getValue());
        break;
      case LONG_GAUGE:
        pointBuilder.setValue((double) ((LongPointData) pointData).getValue());
        break;
      case DOUBLE_SUM:
        pointBuilder.setValue(((DoublePointData) pointData).getValue());
        break;
      case DOUBLE_GAUGE:
        pointBuilder.setValue(((DoublePointData) pointData).getValue());
        break;
      case HISTOGRAM:
        long histogramCount = ((HistogramPointData) pointData).getCount();
        if (histogramCount <= Integer.MAX_VALUE && histogramCount >= Integer.MIN_VALUE) {
          pointBuilder.setCount((int) histogramCount);
        }
        HistogramPointData histogramPointData = (HistogramPointData) pointData;
        pointBuilder.setValue(histogramPointData.getSum());
        pointBuilder.setMin(histogramPointData.getMin());
        pointBuilder.setMax(histogramPointData.getMax());
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
  }
}
