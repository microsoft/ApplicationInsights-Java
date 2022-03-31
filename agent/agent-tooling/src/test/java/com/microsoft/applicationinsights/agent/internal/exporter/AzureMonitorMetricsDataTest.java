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
import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
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
            .build(); // buildAndRegisterGlobal can be called only once ->
    // GlobalOpenTelemetry.get();

    meter =
        openTelemetry
            .meterBuilder("AzureMonitorMetricsDataTest")
            .setInstrumentationVersion("1.0.0")
            .build();
  }

  @AfterEach
  public void cleanup() {
    inMemoryMetricExporter.reset();
  }

  @Test
  public void testDoubleCounter() throws InterruptedException {
    DoubleCounter counter = meter.counterBuilder("testDoubleCounter").ofDoubles().build();
    counter.add(3.1415);

    Thread.sleep(60 * 1000); // wait 1 min

    List<MetricData> metricDatas = inMemoryMetricExporter.getExportedMetrics();
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
  public void testDoubleGauge() throws InterruptedException {
    meter
        .gaugeBuilder("testDoubleGauge")
        .setDescription("the current temperature")
        .setUnit("C")
        .buildWithCallback(
            m -> {
              m.record(20.0, Attributes.of(AttributeKey.stringKey("thing"), "engine"));
            });

    Thread.sleep(60 * 1000); // wait 1 min

    List<MetricData> metricDataList = inMemoryMetricExporter.getExportedMetrics();
    assertThat(metricDataList.size()).isEqualTo(1);

    MetricData metricData = metricDataList.get(0);
    for (PointData pointData : metricData.getData().getPoints()) {
      AzureMonitorMetricsData azureMonitorMetricsData =
          new AzureMonitorMetricsData(metricDataList.get(0), pointData);
      List<MetricDataPoint> metricDataPoints =
          azureMonitorMetricsData.getMetricsData().getMetrics();
      assertThat(metricDataPoints.size()).isEqualTo(1);
      assertThat(metricDataPoints.get(0).getValue()).isEqualTo(20.0);
      assertThat(azureMonitorMetricsData.getMetricsData().getProperties().size()).isEqualTo(2);
      assertThat(azureMonitorMetricsData.getMetricsData().getProperties().get("thing"))
          .isEqualTo("engine");
    }

    assertThat(metricData.getType()).isEqualTo(DOUBLE_GAUGE);
    assertThat(metricData.getName()).isEqualTo("testDoubleGauge");
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

    Thread.sleep(60 * 2 * 1000); // wait 2 min

    List<MetricData> metricDataList = inMemoryMetricExporter.getExportedMetrics();
    assertThat(metricDataList.size()).isEqualTo(1);
    MetricData metricData = metricDataList.get(0);
    Collection<LongPointData> points = (Collection<LongPointData>) metricData.getData().getPoints();
    assertThat(points.size()).isEqualTo(3);

    points =
        points.stream()
            .sorted(Comparator.comparing(o -> o.getValue()))
            .collect(Collectors.toList());

    Iterator<LongPointData> iterator = points.iterator();
    LongPointData longPointData1 = iterator.next();
    assertThat(longPointData1.getValue()).isEqualTo(2L);
    assertThat(longPointData1.getAttributes().get(AttributeKey.stringKey("name")))
        .isEqualTo("apple");
    assertThat(longPointData1.getAttributes().get(AttributeKey.stringKey("color")))
        .isEqualTo("green");

    LongPointData longPointData2 = iterator.next();
    assertThat(longPointData2.getValue()).isEqualTo(6L);
    assertThat(longPointData2.getAttributes().get(AttributeKey.stringKey("name")))
        .isEqualTo("apple");
    assertThat(longPointData2.getAttributes().get(AttributeKey.stringKey("color")))
        .isEqualTo("red");

    LongPointData longPointData3 = iterator.next();
    assertThat(longPointData3.getValue()).isEqualTo(7L);
    assertThat(longPointData3.getAttributes().get(AttributeKey.stringKey("name")))
        .isEqualTo("lemon");
    assertThat(longPointData3.getAttributes().get(AttributeKey.stringKey("color")))
        .isEqualTo("yellow");

    AzureMonitorMetricsData azureMonitorMetricsData =
        new AzureMonitorMetricsData(metricData, longPointData1);
    List<MetricDataPoint> metricDataPoints = azureMonitorMetricsData.getMetricsData().getMetrics();
    assertThat(metricDataPoints.size()).isEqualTo(1);
    MetricDataPoint metricDataPoint = metricDataPoints.get(0);
    assertThat(metricDataPoint.getValue()).isEqualTo(2L);
    assertThat(metricDataPoint.getDataPointType()).isEqualTo(DataPointType.AGGREGATION);

    Map<String, String> properties = azureMonitorMetricsData.getMetricsData().getProperties();
    assertThat(properties.size()).isEqualTo(3);
    assertThat(properties.get("name")).isEqualTo("apple");
    assertThat(properties.get("color")).isEqualTo("green");
    assertThat(properties.get("_MS.AggregationIntervalMs")).isEqualTo("60000");

    azureMonitorMetricsData = new AzureMonitorMetricsData(metricData, longPointData2);
    metricDataPoints = azureMonitorMetricsData.getMetricsData().getMetrics();
    assertThat(metricDataPoints.size()).isEqualTo(1);
    metricDataPoint = metricDataPoints.get(0);
    assertThat(metricDataPoint.getValue()).isEqualTo(6L);
    assertThat(metricDataPoint.getDataPointType()).isEqualTo(DataPointType.AGGREGATION);

    properties = azureMonitorMetricsData.getMetricsData().getProperties();
    assertThat(properties.size()).isEqualTo(3);
    assertThat(properties.get("name")).isEqualTo("apple");
    assertThat(properties.get("color")).isEqualTo("red");
    assertThat(properties.get("_MS.AggregationIntervalMs")).isEqualTo("60000");

    azureMonitorMetricsData = new AzureMonitorMetricsData(metricData, longPointData3);
    metricDataPoints = azureMonitorMetricsData.getMetricsData().getMetrics();
    assertThat(metricDataPoints.size()).isEqualTo(1);
    metricDataPoint = metricDataPoints.get(0);
    assertThat(metricDataPoint.getValue()).isEqualTo(7L);
    assertThat(metricDataPoint.getDataPointType()).isEqualTo(DataPointType.AGGREGATION);

    properties = azureMonitorMetricsData.getMetricsData().getProperties();
    assertThat(properties.size()).isEqualTo(3);
    assertThat(properties.get("name")).isEqualTo("lemon");
    assertThat(properties.get("color")).isEqualTo("yellow");
    assertThat(properties.get("_MS.AggregationIntervalMs")).isEqualTo("60000");

    assertThat(metricData.getType()).isEqualTo(LONG_SUM);
    assertThat(metricData.getName()).isEqualTo("testLongCounter");
  }

  @Test
  public void testLongGauge() throws InterruptedException {
    meter
        .gaugeBuilder("testLongGauge")
        .ofLongs()
        .setDescription("the current temperature")
        .setUnit("C")
        .buildWithCallback(
            m -> {
              m.record(20, Attributes.of(AttributeKey.stringKey("weather"), "seattle"));
            });

    Thread.sleep(60 * 1000); // wait 1 min

    List<MetricData> metricDataList = inMemoryMetricExporter.getExportedMetrics();
    assertThat(metricDataList.size()).isEqualTo(1);

    MetricData metricData = metricDataList.get(0);
    for (PointData pointData : metricData.getData().getPoints()) {
      AzureMonitorMetricsData azureMonitorMetricsData =
          new AzureMonitorMetricsData(metricDataList.get(0), pointData);
      List<MetricDataPoint> metricDataPoints =
          azureMonitorMetricsData.getMetricsData().getMetrics();
      assertThat(metricDataPoints.size()).isEqualTo(1);
      assertThat(metricDataPoints.get(0).getValue()).isEqualTo(20L);
      assertThat(azureMonitorMetricsData.getMetricsData().getProperties().size()).isEqualTo(2);
      assertThat(azureMonitorMetricsData.getMetricsData().getProperties().get("weather"))
          .isEqualTo("seattle");
    }

    assertThat(metricData.getType()).isEqualTo(LONG_GAUGE);
    assertThat(metricData.getName()).isEqualTo("testLongGauge");
  }
}
