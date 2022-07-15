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

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.FluxUtil;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.MockLogData;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.TestUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class AzureMonitorExportersTest extends MonitorExporterClientTestBase {

  @SystemStub EnvironmentVariables envVars = new EnvironmentVariables();

  private static final String TRACE_CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000000";
  private static final String METRIC_CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000001";
  private static final String INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEF";

  private InMemoryMetricExporter inMemoryMetricExporter;

  @BeforeEach
  public void setup() throws InterruptedException {
    envVars.set(
        "APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=" + INSTRUMENTATION_KEY);
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
  public void testBuildMetricExporter() throws Exception {
    CountDownLatch exporterCountDown = new CountDownLatch(1);
    CustomValidationPolicy customValidationPolicy = new CustomValidationPolicy(exporterCountDown);
    Meter meter = TestUtils.configureAzureMonitorMetricExporter(customValidationPolicy);
    LongCounter counter = meter.counterBuilder("testBuildMetricExporter").build();
    counter.add(
        1L,
        Attributes.of(
            AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    exporterCountDown.await(60, TimeUnit.SECONDS);

    TelemetryItem actualTelemetryItem = customValidationPolicy.getActualTelemetryItem();
    TelemetryItem expectedTelemetryItem =
        TestUtils.createAzureMonitorMetricTelemetry(
            "testBuildMetricExporter", 1, INSTRUMENTATION_KEY, actualTelemetryItem.getTime());
    System.out.println(
        "###### expectedTelemetryItem: \n" + convertTelemetryItemToJson(expectedTelemetryItem));
    System.out.println(
        "###### actualTelemetryitem: \n" + convertTelemetryItemToJson(actualTelemetryItem));
    assertThat(actualTelemetryItem.getName()).isEqualTo(expectedTelemetryItem.getName());
    assertThat(actualTelemetryItem.getInstrumentationKey())
        .isEqualTo(expectedTelemetryItem.getInstrumentationKey());
    assertThat(actualTelemetryItem.getTags()).isEqualTo(expectedTelemetryItem.getTags());
    assertThat(actualTelemetryItem.getData().getBaseType())
        .isEqualTo(expectedTelemetryItem.getData().getBaseType());
    MetricsData expectedMetricsData = (MetricsData) expectedTelemetryItem.getData().getBaseData();
    MetricsData actualMetricsData = (MetricsData) actualTelemetryItem.getData().getBaseData();
    assertThat(actualMetricsData.getMetrics().get(0).getValue())
        .isEqualTo(expectedMetricsData.getMetrics().get(0).getValue());
    assertThat(actualMetricsData.getAdditionalProperties())
        .isEqualTo(expectedMetricsData.getAdditionalProperties());
  }

  // OpenTelemetry doesn't have a Log API
  @Test
  public void testBuildLogExporter() {
    AzureMonitorLogExporter azureMonitorLogExporter =
        getClientBuilder().connectionString(TRACE_CONNECTION_STRING).buildLogExporter();
    CompletableResultCode export =
        azureMonitorLogExporter.export(Collections.singleton(new MockLogData()));
    export.join(30, TimeUnit.SECONDS);
    Assertions.assertTrue(export.isDone());
    Assertions.assertTrue(export.isSuccess());
  }

  @Test
  public void testBuildTraceMetricLogExportersConsecutively() {
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

    AzureMonitorLogExporter azureMonitorLogExporter =
        getClientBuilder().connectionString(TRACE_CONNECTION_STRING).buildLogExporter();
    export = azureMonitorLogExporter.export(Collections.singleton(new MockLogData()));
    export.join(30, TimeUnit.SECONDS);
    Assertions.assertTrue(export.isDone());
    Assertions.assertTrue(export.isSuccess());
  }

  private static class CustomValidationPolicy implements HttpPipelinePolicy {

    private final CountDownLatch countDown;
    private TelemetryItem actualTelemetryItem;

    CustomValidationPolicy(CountDownLatch countDown) {
      this.countDown = countDown;
    }

    public TelemetryItem getActualTelemetryItem() {
      return actualTelemetryItem;
    }

    @Override
    public Mono<HttpResponse> process(
        HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
      Mono<String> asyncBytes =
          FluxUtil.collectBytesInByteBufferStream(context.getHttpRequest().getBody())
              .map(
                  bytes -> {
                    return ungzip(bytes);
                  });
      asyncBytes.subscribe(
          value -> {
            try {
              ObjectMapper objectMapper = createObjectMapper();
              actualTelemetryItem = objectMapper.readValue(value, TelemetryItem.class);
              countDown.countDown();
            } catch (Exception e) {
              // e.printStackTrace();
            }
          });
      return next.process();
    }

    // decode gzipped request raw bytes back to original request body
    private static String ungzip(byte[] rawBytes) {
      try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(rawBytes))) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int read;
        while ((read = in.read(data, 0, data.length)) != -1) {
          baos.write(data, 0, read);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
      } catch (Exception e) {
        return null;
      }
    }
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.registerModules(ObjectMapper.findModules(TelemetryItem.class.getClassLoader()));
    return mapper;
  }

  private static String convertTelemetryItemToJson(TelemetryItem telemetryItem) throws Exception {
    ObjectMapper mapper = createObjectMapper();
    StringWriter stringWriter = new StringWriter();
    mapper.writeValue(mapper.createGenerator(stringWriter), telemetryItem);
    return stringWriter.toString();
  }
}
