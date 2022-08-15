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

import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCIES_DURATION;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCY_RESULT_CODE;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCY_SUCCESS;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCY_TYPE;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.BaseExtractor.FALSE;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.BaseExtractor.MS_IS_AUTOCOLLECTED;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.BaseExtractor.MS_METRIC_ID;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.BaseExtractor.MS_PROCESSED_BY_METRIC_EXTRACTORS;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.BaseExtractor.OPERATION_SYNTHETIC;
import static com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics.BaseExtractor.TRUE;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import com.azure.monitor.opentelemetry.exporter.implementation.MetricDataMapper;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcClientMetrics;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PreAggregatedMetricsTest {

  private InMemoryMetricReader metricReader;
  private SdkMeterProvider meterProvider;

  @BeforeEach
  void setup() {
    metricReader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
  }

  @SuppressWarnings("SystemOut")
  @Test
  void generateHttpClientMetrics() {
    OperationListener listener = HttpClientMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put("http.method", "GET")
            .put("http.url", "https://localhost:1234/")
            .put("http.host", "host")
            .put("http.target", "/")
            .put("http.scheme", "https")
            .put("net.peer.name", "localhost")
            .put("net.peer.ip", "0.0.0.0")
            .put("net.peer.port", 1234)
            .put("http.request_content_length", 100)
            .build();

    Attributes responseAttributes =
        Attributes.builder()
            .put("http.flavor", "2.0")
            .put("http.server_name", "server")
            .put("http.status_code", 200)
            .put("http.response_content_length", 200)
            .build();

    Context parent =
        Context.root()
            .with(
                Span.wrap(
                    SpanContext.create(
                        "ff01020304050600ff0a0b0c0d0e0f00",
                        "090a0b0c0d0e0f00",
                        TraceFlags.getSampled(),
                        TraceState.getDefault())));

    Context context1 = listener.onStart(parent, requestAttributes, nanos(100));
    listener.onEnd(context1, responseAttributes, nanos(250));

    Collection<MetricData> metricDataCollection = metricReader.collectAllMetrics();
    for (MetricData metricData : metricDataCollection) {
      System.out.println("metric: " + metricData);
    }

    assertThat(metricDataCollection.size()).isEqualTo(1);

    assertThat(metricDataCollection)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("http.client.duration")
                    .hasUnit("ms")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(150 /* millis */)
                                        .hasAttributesSatisfying(
                                            equalTo(SemanticAttributes.NET_PEER_NAME, "localhost"),
                                            equalTo(SemanticAttributes.NET_PEER_PORT, 1234),
                                            equalTo(SemanticAttributes.HTTP_METHOD, "GET"),
                                            equalTo(SemanticAttributes.HTTP_FLAVOR, "2.0"),
                                            equalTo(SemanticAttributes.HTTP_STATUS_CODE, 200))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))));

    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = metricDataCollection.iterator().next();
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(generateExpectedProperties("http"));
  }

  @SuppressWarnings("SystemOut")
  @Test
  void generateRpcClientMetrics() {
    OperationListener listener = RpcClientMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put(SemanticAttributes.RPC_SYSTEM, "grpc")
            .put(SemanticAttributes.RPC_SERVICE, "myservice.EchoService")
            .put(SemanticAttributes.RPC_METHOD, "exampleMethod")
            .build();

    Attributes responseAttributes1 =
        Attributes.builder()
            .put(SemanticAttributes.NET_PEER_NAME, "example.com")
            .put(SemanticAttributes.NET_PEER_IP, "127.0.0.1")
            .put(SemanticAttributes.NET_PEER_PORT, 8080)
            .put(SemanticAttributes.NET_TRANSPORT, "ip_tcp")
            .build();

    Context parent =
        Context.root()
            .with(
                Span.wrap(
                    SpanContext.create(
                        "ff01020304050600ff0a0b0c0d0e0f00",
                        "090a0b0c0d0e0f00",
                        TraceFlags.getSampled(),
                        TraceState.getDefault())));

    Context context1 = listener.onStart(parent, requestAttributes, nanos(100));

    assertThat(metricReader.collectAllMetrics()).isEmpty();

    listener.onEnd(context1, responseAttributes1, nanos(250));

    Collection<MetricData> metricDataCollection = metricReader.collectAllMetrics();
    for (MetricData metricData : metricDataCollection) {
      System.out.println("metric: " + metricData);
    }

    assertThat(metricDataCollection.size()).isEqualTo(1);

    assertThat(metricDataCollection)
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("rpc.client.duration")
                    .hasUnit("ms")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(150 /* millis */)
                                        .hasAttributesSatisfying(
                                            equalTo(SemanticAttributes.RPC_SYSTEM, "grpc"),
                                            equalTo(
                                                SemanticAttributes.RPC_SERVICE,
                                                "myservice.EchoService"),
                                            equalTo(SemanticAttributes.RPC_METHOD, "exampleMethod"),
                                            equalTo(
                                                SemanticAttributes.NET_PEER_NAME, "example.com"),
                                            equalTo(SemanticAttributes.NET_PEER_PORT, 8080),
                                            equalTo(SemanticAttributes.NET_TRANSPORT, "ip_tcp"))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))));

    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = metricDataCollection.iterator().next();
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(generateExpectedProperties("grpc"));
  }

  private static long nanos(int millis) {
    return TimeUnit.MILLISECONDS.toNanos(millis);
  }

  private static Map<String, String> generateExpectedProperties(String type) {
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(MS_METRIC_ID, DEPENDENCIES_DURATION);
    expectedMap.put(MS_IS_AUTOCOLLECTED, TRUE);
    expectedMap.put(MS_PROCESSED_BY_METRIC_EXTRACTORS, TRUE);
    // TODO performance market is updated in HttpClientMetrics
    //    expectedMap.put(PERFORMANCE_BUCKET, "<250ms");
    expectedMap.put(OPERATION_SYNTHETIC, FALSE);
    expectedMap.put(DEPENDENCY_SUCCESS, TRUE);
    if ("http".equals(type)) {
      expectedMap.put(DEPENDENCY_TYPE, "http");
      expectedMap.put(DEPENDENCY_RESULT_CODE, "200");
    } else {
      expectedMap.put(DEPENDENCY_TYPE, "grpc");
    }
    // TODO test cloud_role_name and cloud_role_instance
    //    expectedMap.put(
    //        CLOUD_ROLE_NAME,
    // telemetryItem.getTags().get(ContextTagKeys.AI_CLOUD_ROLE.toString()));
    //    expectedMap.put(
    //        CLOUD_ROLE_INSTANCE,
    //        telemetryItem.getTags().get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString()));
    return expectedMap;
  }
}
