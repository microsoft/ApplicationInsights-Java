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

import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM;
import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.export.MetricReaderFactory;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AzureMonitorMetricsDataTest {

  private Meter meter;
  private InMemoryMetricExporter inMemoryMetricExporter;

  @BeforeEach
  public void setup() {
    inMemoryMetricExporter = new InMemoryMetricExporter();
    MetricReaderFactory metricReaderFactory =
        PeriodicMetricReader.newMetricReaderFactory(inMemoryMetricExporter);
    SdkMeterProvider meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReaderFactory).build();

    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder()
            .setMeterProvider(meterProvider)
            .buildAndRegisterGlobal(); // GlobalOpenTelemetry.get();
    meter =
        openTelemetry
            .meterBuilder("AzureMonitorMetricsDataTest")
            .setInstrumentationVersion("1.0.0")
            .build();
  }

  @Test
  public void testDoubleCounter() throws InterruptedException {
    DoubleCounter counter =
        (DoubleCounter) meter.counterBuilder("testDoubleCounter").ofDoubles().build();
    counter.add(3.1415);

    Thread.sleep(60 * 1000); // wait 1 min

    List<MetricData> metricDatas = inMemoryMetricExporter.getExportedMetricDatas();
    assertThat(metricDatas.size()).isEqualTo(1);

    MetricData metricData = metricDatas.get(0);
    for (PointData pointData : metricData.getData().getPoints()) {
      AzureMonitorMetricsData azureMonitorMetricsData =
          new AzureMonitorMetricsData(metricDatas.get(0), pointData);
      List<MetricDataPoint> metricDataPoints =
          azureMonitorMetricsData.getMetricsData().getMetrics();
      assertThat(metricDataPoints.size()).isEqualTo(1);
      assertThat(metricDataPoints.get(0).getValue()).isEqualTo(3.1415);
    }
    assertThat(metricData.getType()).isEqualTo(DOUBLE_SUM);
    assertThat(metricData.getName()).isEqualTo("testDoubleCounter");
  }

  @Test
  public void testLongCounter() throws InterruptedException {
    LongCounter counter = meter.counterBuilder("testLongCounter").build();
    counter.add(
        1,
        Attributes.of(
            AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    counter.add(
        2,
        Attributes.of(
            AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));
    counter.add(
        1,
        Attributes.of(
            AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));
    counter.add(
        2,
        Attributes.of(
            AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "green"));
    counter.add(
        5,
        Attributes.of(
            AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    counter.add(
        4,
        Attributes.of(
            AttributeKey.stringKey("name"), "lemon", AttributeKey.stringKey("color"), "yellow"));

    Thread.sleep(60 * 1000); // wait 1 min

    List<MetricData> metricDatas = inMemoryMetricExporter.getExportedMetricDatas();
    assertThat(metricDatas.size()).isEqualTo(1);

    MetricData metricData = metricDatas.get(0);
    Collection<LongPointData> points = (Collection<LongPointData>) metricData.getData().getPoints();
    assertThat(points.size()).isEqualTo(3);

    Iterator<LongPointData> iterator = points.iterator();
    LongPointData longPointData1 = iterator.next();
    assertThat(longPointData1.getValue()).isEqualTo(6L);
    assertThat(longPointData1.getAttributes().get(AttributeKey.stringKey("name")))
        .isEqualTo("apple");
    assertThat(longPointData1.getAttributes().get(AttributeKey.stringKey("color")))
        .isEqualTo("red");

    LongPointData longPointData2 = iterator.next();
    assertThat(longPointData2.getValue()).isEqualTo(2L);
    assertThat(longPointData2.getAttributes().get(AttributeKey.stringKey("name")))
        .isEqualTo("apple");
    assertThat(longPointData2.getAttributes().get(AttributeKey.stringKey("color")))
        .isEqualTo("green");

    LongPointData longPointData3 = iterator.next();
    assertThat(longPointData3.getValue()).isEqualTo(7L);
    assertThat(longPointData3.getAttributes().get(AttributeKey.stringKey("name")))
        .isEqualTo("lemon");
    assertThat(longPointData3.getAttributes().get(AttributeKey.stringKey("color")))
        .isEqualTo("yellow");
  }
}
