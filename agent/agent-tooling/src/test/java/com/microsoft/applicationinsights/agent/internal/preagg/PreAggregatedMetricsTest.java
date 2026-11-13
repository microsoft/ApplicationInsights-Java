// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.preagg;

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

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.MetricDataMapper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.init.AiContextKeys;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
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
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import io.opentelemetry.semconv.incubating.RpcIncubatingAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class PreAggregatedMetricsTest {

  private static final Object REQUEST = new Object();

  private InMemoryMetricReader metricReader;
  private SdkMeterProvider meterProvider;
  private InMemorySpanExporter spanExporter;
  private SdkTracerProvider tracerProvider;

  @BeforeEach
  void setup() {
    metricReader = InMemoryMetricReader.create();
    SdkMeterProviderBuilder builder = SdkMeterProvider.builder();
    AiViewRegistry.registerViews(builder);
    meterProvider = builder.registerMetricReader(metricReader).build();
    spanExporter = InMemorySpanExporter.create();
    tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();
  }

  @SuppressWarnings("SystemOut") // legitimate use of System.out for logging/output
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

    Context context1 = start(MetricsState.HTTP_CLIENT, false, listener, parent, requestAttributes);
    end(MetricsState.HTTP_CLIENT, false, listener, context1, responseAttributes);

    Collection<MetricData> metricDataCollection = metricReader.collectAllMetrics();
    metricDataCollection =
        metricDataCollection.stream()
            .sorted(Comparator.comparing(MetricData::getName))
            .collect(Collectors.toList());
    for (MetricData metricData : metricDataCollection) {
      System.out.println("metric: " + metricData);
    }

    assertThat(metricDataCollection)
        .satisfiesExactly(metric ->
            assertThat(metric)
                .hasName("http.client.request.duration")
                .hasUnit("s")
                .hasHistogramSatisfying(histogram ->
                    histogram.hasPointsSatisfying(point ->
                        point
                            .hasSum(0.15 /* seconds */)
                            .hasAttributesSatisfying(
                                equalTo(ServerAttributes.SERVER_ADDRESS, "localhost"),
                                equalTo(ServerAttributes.SERVER_PORT, 1234),
                                equalTo(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200))
                            .hasExemplarsSatisfying(exemplar ->
                                exemplar
                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                    .hasSpanId("090a0b0c0d0e0f00")))));

    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = metricDataCollection.iterator().next();
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true, null);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(
            generateExpectedDependencyCustomDimensions("http", "localhost:1234"));
  }

  @SuppressWarnings("SystemOut") // legitimate use of System.out for logging/output
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

    Context context1 = start(MetricsState.RPC_CLIENT, false, listener, parent, requestAttributes);

    assertThat(metricReader.collectAllMetrics()).isEmpty();

    end(MetricsState.RPC_CLIENT, false, listener, context1, responseAttributes1);

    Collection<MetricData> metricDataCollection = metricReader.collectAllMetrics();
    for (MetricData metricData : metricDataCollection) {
      System.out.println("metric: " + metricData);
    }

    assertThat(metricDataCollection.size()).isEqualTo(1);

    assertThat(metricDataCollection)
        .satisfiesExactlyInAnyOrder(metric ->
            assertThat(metric)
                .hasName("rpc.client.duration")
                .hasUnit("ms")
                .hasHistogramSatisfying(histogram ->
                    histogram.hasPointsSatisfying(point ->
                        point
                            .hasSum(150 /* millis */)
                            .hasAttributesSatisfying(
                                equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                                equalTo(ServerAttributes.SERVER_ADDRESS, "example.com"),
                                equalTo(ServerAttributes.SERVER_PORT, 8080))
                            .hasExemplarsSatisfying(exemplar ->
                                exemplar
                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                    .hasSpanId("090a0b0c0d0e0f00")))));

    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = metricDataCollection.iterator().next();
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true, null);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(
            generateExpectedDependencyCustomDimensions("grpc", "example.com:8080"));
  }

  @SuppressWarnings("SystemOut") // legitimate use of System.out for logging/output
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
    Context context1 = start(MetricsState.HTTP_SERVER, true, listener, parent1, requestAttributes);
    end(MetricsState.HTTP_SERVER, true, listener, context1, responseAttributes);

    Collection<MetricData> metricDataCollection = metricReader.collectAllMetrics();
    MetricData target = null;
    for (MetricData metricData : metricDataCollection) {
      if ("http.server.request.duration".equals(metricData.getName())) {
        target = metricData;
        System.out.println("metric: " + metricData);
      }
    }

    assertThat(target)
        .satisfiesAnyOf(metric ->
            assertThat(metric)
                .hasName("http.server.request.duration")
                .hasUnit("s")
                .hasHistogramSatisfying(histogram ->
                    histogram.hasPointsSatisfying(point ->
                        point
                            .hasSum(0.15 /* seconds */)
                            .hasAttributesSatisfying(
                                equalTo(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200),
                                equalTo(
                                    AttributeKey.booleanKey(
                                        "applicationinsights.internal.is_synthetic"),
                                    false))
                            .hasExemplarsSatisfying(exemplar ->
                                exemplar
                                    .hasTraceId(spanContext1.getTraceId())
                                    .hasSpanId(spanContext1.getSpanId())))));

    listener.onEnd(context1, responseAttributes, nanos(250));
    MetricTelemetryBuilder builder = MetricTelemetryBuilder.create();
    MetricData metricData = target;
    MetricDataMapper.updateMetricPointBuilder(
        builder, metricData, metricData.getData().getPoints().iterator().next(), true, true, null);
    TelemetryItem telemetryItem = builder.build();
    MetricsData metricsData = (MetricsData) telemetryItem.getData().getBaseData();

    assertThat(metricsData.getProperties())
        .containsExactlyInAnyOrderEntriesOf(generateExpectedRequestCustomDimensions("http"));
  }

  @Test
  void httpServerMetricsCaptureSyntheticAndOverrides() {
    OperationListener listener = HttpServerMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put("http.request.method", "GET")
            .put("url.path", "/")
            .put(UserAgentAttributes.USER_AGENT_ORIGINAL, "Mozilla/5.0 (compatible; AlwaysOn)")
            .build();
    Attributes responseAttributes =
        Attributes.builder().put("http.response.status_code", 200).build();

    Span span = tracerProvider.get("test").spanBuilder("GET /").startSpan();
    Context parent =
        Context.root()
            .with(span)
            .with(AiContextKeys.CONNECTION_STRING, "InstrumentationKey=override")
            .with(AiContextKeys.ROLE_NAME, "role-override");
    Context context = start(MetricsState.HTTP_SERVER, true, listener, parent, requestAttributes);
    end(MetricsState.HTTP_SERVER, true, listener, context, responseAttributes);
    span.end();

    assertThat(getMetric("http.server.request.duration"))
        .hasHistogramSatisfying(histogram ->
            histogram.hasPointsSatisfying(point ->
                point.hasAttributesSatisfyingExactly(
                    equalTo(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200),
                    equalTo(AiSemanticAttributes.IS_SYNTHETIC, true),
                    equalTo(
                        AiSemanticAttributes.INTERNAL_CONNECTION_STRING,
                        "InstrumentationKey=override"),
                    equalTo(AiSemanticAttributes.INTERNAL_ROLE_NAME, "role-override"))));

    assertThat(getSingleSpan().getAttributes().get(AiSemanticAttributes.IS_PRE_AGGREGATED))
        .isTrue();
  }

  // e.g. the AWS SDK and RMI instrumenters have rpc/http attributes but don't record the duration
  // metrics, so their spans must not be marked as pre-aggregated
  @ParameterizedTest
  @EnumSource(MetricsState.class)
  void spanIsNotPreAggregatedWhenDurationMetricIsNotRecorded(MetricsState metricsState) {
    PreAggregatedMetricsCustomizer customizer =
        new PreAggregatedMetricsCustomizer(
            Collections.singletonList(metricsState), metricsState == MetricsState.HTTP_SERVER);

    Span span = tracerProvider.get("test").spanBuilder("span").startSpan();
    Context context =
        customizer.onStart(
            Context.root().with(span).with(AiContextKeys.ROLE_NAME, "role-override"),
            REQUEST,
            Attributes.empty());
    AttributesBuilder endAttributes = Attributes.builder();
    customizer.onEnd(endAttributes, context, REQUEST, null, null);
    span.end();

    assertThat(endAttributes.build().isEmpty()).isTrue();
    assertThat(getSingleSpan().getAttributes().get(AiSemanticAttributes.IS_PRE_AGGREGATED))
        .isNull();
  }

  // MetricsState looks up private fields of the upstream metrics classes, this fails if they are
  // renamed when updating the OpenTelemetry instrumentation version
  @ParameterizedTest
  @EnumSource(MetricsState.class)
  void metricsStateIsResolved(MetricsState metricsState) {
    assertThat(metricsState.isResolved()).isTrue();
  }

  @Test
  void missingMetricsStateFieldFallsBackGracefully() {
    assertThat(MetricsState.lookupContextKey(HttpServerMetrics.class, "DOES_NOT_EXIST")).isNull();
  }

  // e.g. when span suppression is disabled, a nested http client instrumenter that doesn't record
  // the duration metric inherits the metrics state of its parent
  @Test
  void nestedSpanWithoutMetricsIsNotMarkedFromInheritedMetricsState() {
    OperationListener listener = HttpClientMetrics.get().create(meterProvider.get("test"));
    Attributes attributes = Attributes.builder().put("http.response.status_code", 200).build();

    Span parentSpan = tracerProvider.get("test").spanBuilder("parent").startSpan();
    Context parentContext =
        start(
            MetricsState.HTTP_CLIENT,
            false,
            listener,
            Context.root().with(parentSpan),
            Attributes.empty());

    PreAggregatedMetricsCustomizer childCustomizer =
        new PreAggregatedMetricsCustomizer(
            Collections.singletonList(MetricsState.HTTP_CLIENT), false);
    Span childSpan =
        tracerProvider.get("test").spanBuilder("child").setParent(parentContext).startSpan();
    Context childContext =
        childCustomizer.onStart(parentContext.with(childSpan), REQUEST, Attributes.empty());
    AttributesBuilder childEndAttributes = Attributes.builder();
    childCustomizer.onEnd(childEndAttributes, childContext, REQUEST, null, null);
    childSpan.end();

    end(MetricsState.HTTP_CLIENT, false, listener, parentContext, attributes);
    parentSpan.end();

    assertThat(spanExporter.getFinishedSpanItems())
        .satisfiesExactlyInAnyOrder(
            span ->
                assertThat(span)
                    .hasName("child")
                    .satisfies(s ->
                        assertThat(s.getAttributes().get(AiSemanticAttributes.IS_PRE_AGGREGATED))
                            .isNull()),
            span ->
                assertThat(span)
                    .hasName("parent")
                    .hasAttribute(AiSemanticAttributes.IS_PRE_AGGREGATED, true));
  }

  // mirrors the order in which Instrumenter calls the context customizers, attributes extractors
  // and operation listeners
  private static Context start(
      MetricsState metricsState,
      boolean captureSynthetic,
      OperationListener listener,
      Context parentContext,
      Attributes startAttributes) {
    PreAggregatedMetricsCustomizer customizer =
        new PreAggregatedMetricsCustomizer(
            Collections.singletonList(metricsState), captureSynthetic);
    Context context = customizer.onStart(parentContext, REQUEST, startAttributes);
    return listener.onStart(context, startAttributes, nanos(100));
  }

  private static void end(
      MetricsState metricsState,
      boolean captureSynthetic,
      OperationListener listener,
      Context context,
      Attributes endAttributes) {
    PreAggregatedMetricsCustomizer customizer =
        new PreAggregatedMetricsCustomizer(
            Collections.singletonList(metricsState), captureSynthetic);
    AttributesBuilder attributes = endAttributes.toBuilder();
    customizer.onEnd(attributes, context, REQUEST, null, null);
    listener.onEnd(context, attributes.build(), nanos(250));
  }

  private MetricData getMetric(String name) {
    return metricReader.collectAllMetrics().stream()
        .filter(metricData -> metricData.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new AssertionError("metric not found: " + name));
  }

  private SpanData getSingleSpan() {
    assertThat(spanExporter.getFinishedSpanItems()).hasSize(1);
    return spanExporter.getFinishedSpanItems().get(0);
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
