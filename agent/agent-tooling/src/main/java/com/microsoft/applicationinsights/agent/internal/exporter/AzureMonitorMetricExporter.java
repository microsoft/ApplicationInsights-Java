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

import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE;
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM;

import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorBase;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureMonitorMetricExporter implements MetricExporter {

  private final TelemetryClient telemetryClient;
  private static final Logger logger = LoggerFactory.getLogger(AzureMonitorMetricExporter.class);

  public AzureMonitorMetricExporter(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    for (MetricData metricData : metrics) {
      MetricDataType type = metricData.getType();
      if (type == DOUBLE_SUM || type == DOUBLE_GAUGE || type == LONG_SUM || type == LONG_GAUGE) {
        TelemetryItem item = convertOtelMetricToAzureMonitorMetric(metricData);
        telemetryClient.trackAsync(item);
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
    return CompletableResultCode.ofSuccess();
  }

  // visible for testing
  TelemetryItem convertOtelMetricToAzureMonitorMetric(MetricData metricData) {
    TelemetryItem telemetryItem = new TelemetryItem();
    telemetryItem.setInstrumentationKey(telemetryClient.getInstrumentationKey());
    for (PointData data : metricData.getData().getPoints()) {
      MonitorBase monitorBase = new MonitorBase();
      monitorBase.setBaseType("MetricData");
      AzureMonitorMetricsData azureMonitorMetricsData =
          new AzureMonitorMetricsData(metricData, data);
      MetricsData metricsData = azureMonitorMetricsData.getMetricsData();
      populateDefaults(telemetryItem, metricsData);
      monitorBase.setBaseData(azureMonitorMetricsData.getMetricsData());
      telemetryItem.setData(monitorBase);
    }

    return telemetryItem;
  }

  // visible for testing
  void populateDefaults(TelemetryItem telemetryItem, MetricsData metricsData) {
    telemetryItem.setInstrumentationKey(telemetryClient.getInstrumentationKey());
    Map<String, String> tags = telemetryItem.getTags();
    Map<String, String> globalTags = telemetryClient.getGlobalTags();
    if (tags == null && !globalTags.isEmpty()) {
      tags = new HashMap<>();
    }
    for (Map.Entry<String, String> entry : globalTags.entrySet()) {
      tags.put(entry.getKey(), entry.getValue());
    }
    if (!globalTags.isEmpty()) {
      telemetryItem.setTags(tags);
    }

    Map<String, String> globalProperties = telemetryClient.getGlobalProperties();
    Map<String, String> properties = metricsData.getProperties();
    if (properties == null && !globalProperties.isEmpty()) {
      properties = new HashMap<>();
    }
    for (Map.Entry<String, String> entry : globalProperties.entrySet()) {
      properties.put(entry.getKey(), entry.getValue());
    }
    if (!globalProperties.isEmpty()) {
      metricsData.setProperties(properties);
    }
  }
}
