// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal;

import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCIES_DURATION;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCY_RESULT_CODE;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCY_SUCCESS;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCY_TARGET;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.DependencyExtractor.DEPENDENCY_TYPE;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.ExtractorHelper.FALSE;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.ExtractorHelper.MS_IS_AUTOCOLLECTED;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.ExtractorHelper.MS_METRIC_ID;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.ExtractorHelper.OPERATION_SYNTHETIC;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.ExtractorHelper.TRUE;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.RequestExtractor.REQUESTS_DURATION;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.RequestExtractor.REQUEST_RESULT_CODE;
import static com.azure.monitor.opentelemetry.autoconfigure.implementation.preaggregatedmetrics.RequestExtractor.REQUEST_SUCCESS;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.MetricDataMapper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerMetrics;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.view.AiViewRegistry;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.incubating.RpcIncubatingAttributes;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PreAggregatedMetricsTest {

  private InMemoryMetricReader metricReader;
  private SdkMeterProvider meterProvider;

  @BeforeEach
  void setup() {
    metricReader = InMemoryMetricReader.create();
    SdkMeterProviderBuilder builder = SdkMeterProvider.builder();
    AiViewRegistry.registerViews(builder);
    meterProvider = builder.registerMetricReader(metricReader).build();
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
            .put("server.address", "localhost")
            .put("server.port", 1234)
            .put("http.request_content_length", 100)
            .build();

    Attributes responseAttributes =
        Attributes.builder()
            .put("http.flavor", "2.0")
            .put("http.server_name", "server")
            .put("http.response.status_code", 200)
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
    metricDataCollection =
        metricDataCollection.stream()
            .sorted(Comparator.comparing(MetricData::getName))
            .collect(Collectors.toList());
    for (MetricData metricData : metricDataCollection) {
      System.out.println("metric: " + metricData);
    }

    assertThat(metricDataCollection)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("http.client.request.duration")
                    .hasUnit("s")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(0.15 /* seconds */)
                                        .hasAttributesSatisfying(
                                            equalTo(ServerAttributes.SERVER_ADDRESS, "localhost"),
                                            equalTo(ServerAttributes.SERVER_PORT, 1234),
                                            equalTo(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))));

    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = metricDataCollection.iterator().next();
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true, false);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(
            generateExpectedDependencyCustomDimensions("http", "localhost:1234"));
  }

  @SuppressWarnings("SystemOut")
  @Test
  void generateRpcClientMetrics() {
    OperationListener listener = RpcClientMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put(RpcIncubatingAttributes.RPC_SYSTEM, "grpc")
            .put(RpcIncubatingAttributes.RPC_SERVICE, "myservice.EchoService")
            .put(RpcIncubatingAttributes.RPC_METHOD, "exampleMethod")
            .build();

    Attributes responseAttributes1 =
        Attributes.builder()
            .put(ServerAttributes.SERVER_ADDRESS, "example.com")
            .put(ServerAttributes.SERVER_PORT, 8080)
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
                                            equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                                            equalTo(ServerAttributes.SERVER_ADDRESS, "example.com"),
                                            equalTo(ServerAttributes.SERVER_PORT, 8080))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))));

    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = metricDataCollection.iterator().next();
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true, false);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(
            generateExpectedDependencyCustomDimensions("grpc", "example.com:8080"));
  }

  @SuppressWarnings("SystemOut")
  @Test
  void generateHttpServerMetrics() {
    OperationListener listener = HttpServerMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put("http.method", "GET")
            .put("http.host", "host")
            .put("http.target", "/")
            .put("http.scheme", "https")
            .put("server.address", "localhost")
            .put("server.port", 1234)
            .put("http.request_content_length", 100)
            .build();

    Attributes responseAttributes =
        Attributes.builder()
            .put("http.flavor", "2.0")
            .put("http.server_name", "server")
            .put("http.response.status_code", 200)
            .put("http.response_content_length", 200)
            .build();

    SpanContext spanContext1 =
        SpanContext.create(
            "ff01020304050600ff0a0b0c0d0e0f00",
            "090a0b0c0d0e0f00",
            TraceFlags.getSampled(),
            TraceState.getDefault());
    Context parent1 = Context.root().with(Span.wrap(spanContext1));
    Context context1 = listener.onStart(parent1, requestAttributes, nanos(100));
    listener.onEnd(context1, responseAttributes, nanos(250));

    Collection<MetricData> metricDataCollection = metricReader.collectAllMetrics();
    MetricData target = null;
    for (MetricData metricData : metricDataCollection) {
      if ("http.server.request.duration".equals(metricData.getName())) {
        target = metricData;
        System.out.println("metric: " + metricData);
      }
    }

    assertThat(target)
        .satisfiesAnyOf(
            metric ->
                assertThat(metric)
                    .hasName("http.server.request.duration")
                    .hasUnit("s")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(0.15 /* seconds */)
                                        .hasAttributesSatisfying(
                                            equalTo(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200),
                                            equalTo(
                                                AttributeKey.booleanKey(
                                                    "applicationinsights.internal.is_synthetic"),
                                                false))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId(spanContext1.getTraceId())
                                                    .hasSpanId(spanContext1.getSpanId())))));

    listener.onEnd(context1, responseAttributes, nanos(250));
    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = target;
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true, false);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(generateExpectedRequestCustomDimensions("http"));
  }

  private static long nanos(int millis) {
    return TimeUnit.MILLISECONDS.toNanos(millis);
  }

  private static Map<String, String> generateExpectedDependencyCustomDimensions(
      String type, String target) {
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(MS_METRIC_ID, DEPENDENCIES_DURATION);
    expectedMap.put(MS_IS_AUTOCOLLECTED, TRUE);
    expectedMap.put(OPERATION_SYNTHETIC, FALSE);
    expectedMap.put(DEPENDENCY_SUCCESS, TRUE);
    if ("http".equals(type)) {
      expectedMap.put(DEPENDENCY_TYPE, "Http");
      expectedMap.put(DEPENDENCY_RESULT_CODE, "200");
    } else {
      expectedMap.put(DEPENDENCY_TYPE, "grpc");
    }
    expectedMap.put(DEPENDENCY_TARGET, target);
    // TODO test cloud_role_name and cloud_role_instance
    //    expectedMap.put(
    //        CLOUD_ROLE_NAME,
    // telemetryItem.getTags().get(ContextTagKeys.AI_CLOUD_ROLE.toString()));
    //    expectedMap.put(
    //        CLOUD_ROLE_INSTANCE,
    //        telemetryItem.getTags().get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString()));
    return expectedMap;
  }

  private static Map<String, String> generateExpectedRequestCustomDimensions(String type) {
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put(MS_METRIC_ID, REQUESTS_DURATION);
    expectedMap.put(MS_IS_AUTOCOLLECTED, TRUE);
    expectedMap.put(OPERATION_SYNTHETIC, FALSE);
    expectedMap.put(REQUEST_SUCCESS, TRUE);
    if ("http".equals(type)) {
      expectedMap.put(REQUEST_RESULT_CODE, "200");
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
