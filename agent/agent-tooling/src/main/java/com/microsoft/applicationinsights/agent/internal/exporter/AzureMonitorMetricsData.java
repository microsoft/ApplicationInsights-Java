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

import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricPointBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;

final class AzureMonitorMetricsData {

  private static final String DEFAULT_METRIC_AGGREGATION_INTERVAL = "60000";
  private static final String AGGREGATION_INTERNAL_MS_KEY = "_MS.AggregationIntervalMs";

  public static void populatePointAndProperties(
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

  private AzureMonitorMetricsData() {}
}
