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

import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.opentelemetry.api.internal.Utils.checkArgument;

final class AzureMonitorMetricsData {

  private static final int VERSION = 2;
  private static final Logger logger = LoggerFactory.getLogger(AzureMonitorMetricsData.class);
  private MetricsData metricsData;

  public AzureMonitorMetricsData(MetricData metricData, PointData pointData) {
    checkArgument(metricData != null, "MetricData cannot be null.");

    metricsData = new MetricsData();
    metricsData.setVersion(VERSION);
    List<MetricDataPoint> metricDataPoints = new ArrayList<>();
    MetricDataPoint metricDataPoint = new MetricDataPoint();
    MetricDataType type = metricData.getType();
    switch(type) {
      case LONG_SUM:
        metricDataPoint.setDataPointType(DataPointType.AGGREGATION);
        metricDataPoint.setValue(((LongPointData) pointData).getValue());
        break;
      case LONG_GAUGE:
        metricDataPoint.setDataPointType(DataPointType.MEASUREMENT);
        metricDataPoint.setValue(((LongPointData) pointData).getValue());
        break;
      case DOUBLE_SUM:
        metricDataPoint.setDataPointType(DataPointType.AGGREGATION);
        metricDataPoint.setValue(((DoublePointData) pointData).getValue());
        break;
      case DOUBLE_GAUGE:
        metricDataPoint.setDataPointType(DataPointType.MEASUREMENT);
        metricDataPoint.setValue(((DoublePointData) pointData).getValue());
        break;
      case SUMMARY: // not supported yet in OpenTelemetry SDK
      case HISTOGRAM: // supported in OpenTelemetry SDK but not supported yet in Breeze
      case EXPONENTIAL_HISTOGRAM: // not supported yet in OpenTelemetry SDK
      default:
        throw new IllegalArgumentException("metric data type '" + type + "' is not supported yet");
    }

    metricDataPoint.setName(metricData.getName());
    metricDataPoints.add(metricDataPoint);
    metricsData.setMetrics(metricDataPoints);

    if (pointData.getAttributes() != null) {
      Map<String, String> properties = new HashMap<>();
      for (AttributeKey key : pointData.getAttributes().asMap().keySet()) {
        properties.put(key.getKey(), pointData.getAttributes().get(key).toString());
      }
      metricsData.setProperties(properties);
    }
  }

  public MetricsData getMetricsData() {
      return metricsData;
    }
}
