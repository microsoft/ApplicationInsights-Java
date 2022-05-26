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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AzureMonitorExportersTest extends MonitorExporterClientTestBase {

  private static final String TRACE_CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000000";
  private static final String METRIC_CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000001";

  private InMemoryMetricExporter inMemoryMetricExporter;

  @BeforeEach
  public void setup() throws InterruptedException {
    inMemoryMetricExporter = InMemoryMetricExporter.create();
    PeriodicMetricReader metricReader =
        PeriodicMetricReader.builder(inMemoryMetricExporter)
            .setInterval(Duration.ofMillis(10))
            .build();
    SdkMeterProvider meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReader).build();

    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();

    Meter meter =
        openTelemetry
            .meterBuilder("AzureMonitorMetricExporterTest")
            .setInstrumentationVersion("1.0.0")
            .build();
    DoubleHistogram doubleHistogram =
        meter
            .histogramBuilder("testDoubleHistogram")
            .setDescription("http.client.duration")
            .setUnit("ms")
            .build();

    doubleHistogram.record(25.45);
    Thread.sleep(1000);
  }

  @Test
  public void testBuildMetricExporter() {
    AzureMonitorMetricExporter azureMonitorMetricExporter =
        getClientBuilder().connectionString(METRIC_CONNECTION_STRING).buildMetricExporter();
    List<MetricData> metricDataList = inMemoryMetricExporter.getFinishedMetricItems();
    Assertions.assertTrue(metricDataList.size() > 0);
    CompletableResultCode export = azureMonitorMetricExporter.export(metricDataList);
    export.join(30, TimeUnit.SECONDS);
    Assertions.assertTrue(export.isDone());
    Assertions.assertTrue(export.isSuccess());
  }

  @Test
  public void testBuildTraceExporterAndBuildMetricExporterConsecutively() {
    AzureMonitorTraceExporter azureMonitorTraceExporter =
        getClientBuilder().connectionString(TRACE_CONNECTION_STRING).buildTraceExporter();
    CompletableResultCode export =
        azureMonitorTraceExporter.export(
            Collections.singleton(new AzureMonitorTraceExporterTest.RequestSpanData()));
    export.join(30, TimeUnit.SECONDS);
    Assertions.assertTrue(export.isDone());
    Assertions.assertTrue(export.isSuccess());

    AzureMonitorMetricExporter azureMonitorMetricExporter =
        getClientBuilder().connectionString(METRIC_CONNECTION_STRING).buildMetricExporter();
    List<MetricData> metricDataList = inMemoryMetricExporter.getFinishedMetricItems();
    Assertions.assertTrue(metricDataList.size() > 0);
    export = azureMonitorMetricExporter.export(metricDataList);
    export.join(30, TimeUnit.SECONDS);
    Assertions.assertTrue(export.isDone());
    Assertions.assertTrue(export.isSuccess());
  }
}
