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
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.Severity;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  @Test
  public void testResourceDefaultWebsiteInstanceId() throws InterruptedException {
    envVars.set("WEBSITE_SITE_NAME", "test_website_site_name");
    envVars.set("WEBSITE_INSTANCE_ID", "test_website_instance_id");
    assertThat(System.getenv("WEBSITE_SITE_NAME")).isEqualTo("test_website_site_name");
    assertThat(System.getenv("WEBSITE_INSTANCE_ID")).isEqualTo("test_website_instance_id");
    envVars.set(
        "APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=" + INSTRUMENTATION_KEY);
    CountDownLatch exporterCountDown = new CountDownLatch(1);
    Meter meter =
        configureAzureMonitorMetricExporter(new CustomValidationPolicy(exporterCountDown));
    LongCounter counter =
        meter.counterBuilder("testResourceWebsiteSiteNameAndWebsiteInstanceId").build();
    counter.add(
        1L,
        Attributes.of(
            AttributeKey.stringKey("name"), "apple", AttributeKey.stringKey("color"), "red"));
    Thread.sleep(1000);
  }

  static class MockLogData implements LogData {

    @Override
    public Resource getResource() {
      return Resource.create(Attributes.empty());
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
      return InstrumentationScopeInfo.create("Testing Instrumentation", "1", null);
    }

    @Override
    public long getEpochNanos() {
      return Instant.now().getEpochSecond();
    }

    @Override
    public SpanContext getSpanContext() {
      return SpanContext.create(
          TraceId.fromLongs(10L, 2L),
          SpanId.fromLong(1),
          TraceFlags.getDefault(),
          TraceState.builder().build());
    }

    @Override
    public Severity getSeverity() {
      return Severity.DEBUG;
    }

    @Override
    public String getSeverityText() {
      return "DEBUG";
    }

    @Override
    public Body getBody() {
      return Body.string("testing log");
    }

    @Override
    public Attributes getAttributes() {
      return Attributes.builder()
          .put("one", "1")
          .put("two", 2L)
          .put("db.svc", "location")
          .put("operation", "get")
          .put("id", "1234")
          .build();
    }
  }

  private static class CustomValidationPolicy implements HttpPipelinePolicy {

    private final CountDownLatch countDown;

    CustomValidationPolicy(CountDownLatch countDown) {
      this.countDown = countDown;
    }

    @Override
    public Mono<HttpResponse> process(
        HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
      Mono<byte[]> asyncBytes =
          FluxUtil.collectBytesInByteBufferStream(context.getHttpRequest().getBody());
      asyncBytes.subscribe(
          bytes -> {
            try {
              String rawJson = ungzip(bytes);
              ObjectMapper objectMapper = createObjectMapper();
              TelemetryItem actualTelemetryItem =
                  objectMapper.readValue(rawJson, TelemetryItem.class);
              Map<String, String> tags = actualTelemetryItem.getTags();
              if (tags.get(ContextTagKeys.AI_CLOUD_ROLE.toString()).equals("unknown_service:java")
                  && tags.get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString())
                      .equals("test_website_site_name")) {
                countDown.countDown();
              }
            } catch (Exception ignore) {
              // ignore.printStackTrace();
            }
          });
      return next.process();
    }

    // decode gzipped request raw bytes back to original request body
    private static String ungzip(byte[] rawBytes) throws Exception {
      try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(rawBytes))) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int read;
        while ((read = in.read(data, 0, data.length)) != -1) {
          baos.write(data, 0, read);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
      }
    }

    private static ObjectMapper createObjectMapper() {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      mapper.registerModules(ObjectMapper.findModules(TelemetryItem.class.getClassLoader()));
      return mapper;
    }
  }

  protected Meter configureAzureMonitorMetricExporter(HttpPipelinePolicy policy) {
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
}
