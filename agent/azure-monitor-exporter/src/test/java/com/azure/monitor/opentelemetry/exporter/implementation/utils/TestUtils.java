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

package com.azure.monitor.opentelemetry.exporter.implementation.utils;

import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;
import com.azure.monitor.opentelemetry.exporter.AzureMonitorMetricExporter;
import com.azure.monitor.opentelemetry.exporter.AzureMonitorTraceExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorBase;
import com.azure.monitor.opentelemetry.exporter.implementation.models.RemoteDependencyData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestUtils {

  public static TelemetryItem createMetricTelemetry(
      String name, int value, String instrumentationKey) {
    TelemetryItem telemetry = new TelemetryItem();
    telemetry.setVersion(1);
    telemetry.setName("Metric");
    telemetry.setInstrumentationKey(instrumentationKey);
    Map<String, String> tags = new HashMap<>();
    tags.put("ai.internal.sdkVersion", "test_version");
    tags.put("ai.internal.nodeName", "test_role_name");
    tags.put("ai.cloud.roleInstance", "test_cloud_name");
    telemetry.setTags(tags);

    MetricsData data = new MetricsData();
    List<MetricDataPoint> dataPoints = new ArrayList<>();
    MetricDataPoint dataPoint = new MetricDataPoint();
    dataPoint.setName(name);
    dataPoint.setValue(value);
    dataPoint.setCount(1);
    dataPoints.add(dataPoint);

    Map<String, String> properties = new HashMap<>();
    properties.put("state", "blocked");

    data.setMetrics(dataPoints);
    data.setProperties(properties);

    MonitorBase monitorBase = new MonitorBase();
    monitorBase.setBaseType("MetricData");
    monitorBase.setBaseData(data);
    telemetry.setData(monitorBase);
    telemetry.setTime(FormattedTime.offSetDateTimeFromNow());

    return telemetry;
  }

  public static TelemetryItem createAzureMonitorMetricTelemetry(
      String name, int value, String instrumentationKey, OffsetDateTime time, String sdkVersion) {
    TelemetryItem telemetry = new TelemetryItem();
    telemetry.setVersion(1);
    telemetry.setName("Metric");
    telemetry.setInstrumentationKey(instrumentationKey);
    Map<String, String> tags = new HashMap<>();
    tags.put("ai.internal.sdkVersion", sdkVersion);
    tags.put("ai.cloud.role", "unknown_service:java");
    telemetry.setTags(tags);

    MetricsData data = new MetricsData();
    data.setVersion(2);
    List<MetricDataPoint> dataPoints = new ArrayList<>();
    MetricDataPoint dataPoint = new MetricDataPoint();
    dataPoint.setName(name);
    dataPoint.setValue(value);
    dataPoints.add(dataPoint);
    Map<String, String> properties = new HashMap<>();
    properties.put("color", "red");
    properties.put("name", "apple");
    data.setMetrics(dataPoints);
    data.setProperties(properties);
    MonitorBase monitorBase = new MonitorBase();
    monitorBase.setBaseType("MetricData");
    monitorBase.setBaseData(data);
    telemetry.setData(monitorBase);
    telemetry.setTime(time);

    return telemetry;
  }

  public static TelemetryItem createAzureMonitorRemoteDependencyTelemetry(
      String name, String instrumentationKey, OffsetDateTime time, String operationId) {
    TelemetryItem telemetry = new TelemetryItem();
    telemetry.setVersion(1);
    telemetry.setName("RemoteDependency");
    telemetry.setInstrumentationKey(instrumentationKey);
    Map<String, String> tags = new HashMap<>();
    tags.put("ai.internal.sdkVersion", "java11.0.10:otel1.15.0:ext1.0.0-beta.4");
    tags.put("ai.operation.id", operationId);
    tags.put("ai.cloud.role", "unknown_service:java");
    telemetry.setTags(tags);

    RemoteDependencyData data = new RemoteDependencyData();
    data.setVersion(2);
    Map<String, String> properties = new HashMap<>();
    properties.put("color", "red");
    properties.put("name", "apple");
    data.setProperties(properties);
    data.setName(name);
    data.setSuccess(true);
    MonitorBase monitorBase = new MonitorBase();
    monitorBase.setBaseType("RemoteDependencyData");
    monitorBase.setBaseData(data);
    telemetry.setData(monitorBase);
    telemetry.setTime(time);

    return telemetry;
  }

  public static Tracer configureAzureMonitorTraceExporter(HttpPipelinePolicy validator) {
    AzureMonitorTraceExporter exporter =
        new AzureMonitorExporterBuilder()
            .connectionString(System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"))
            .addHttpPipelinePolicy(validator)
            .buildTraceExporter();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();

    OpenTelemetrySdk openTelemetrySdk =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    return openTelemetrySdk.getTracer("Sample");
  }

  public static Meter configureAzureMonitorMetricExporter(HttpPipelinePolicy policy) {
    AzureMonitorMetricExporter exporter =
        new AzureMonitorExporterBuilder()
            .connectionString(System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"))
            .addHttpPipelinePolicy(policy)
            .buildMetricExporter();

    PeriodicMetricReader metricReader =
        PeriodicMetricReader.builder(exporter).setInterval(Duration.ofMillis(10)).build();
    SdkMeterProvider meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();

    return openTelemetry.getMeter("Sample");
  }

  private TestUtils() {}
}
