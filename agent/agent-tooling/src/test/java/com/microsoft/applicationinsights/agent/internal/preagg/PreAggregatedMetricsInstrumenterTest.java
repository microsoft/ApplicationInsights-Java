// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.preagg;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.init.AiContextKeys;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.instrumenter.internal.InstrumenterCustomizerImpl;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.internal.InternalInstrumenterCustomizerProvider;
import io.opentelemetry.instrumentation.api.internal.InternalInstrumenterCustomizerUtil;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.incubating.RpcIncubatingAttributes;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// uses real Instrumenters, so that the order in which the context customizers, attributes
// extractors and operation listeners are called is the one from the OpenTelemetry instrumentation
class PreAggregatedMetricsInstrumenterTest {

  private List<InternalInstrumenterCustomizerProvider> originalCustomizerProviders;
  private boolean emitOldRpcSemconv = true;

  private InMemoryMetricReader metricReader;
  private InMemorySpanExporter spanExporter;
  private OpenTelemetrySdk openTelemetry;

  @BeforeEach
  void setup() {
    originalCustomizerProviders =
        InternalInstrumenterCustomizerUtil.getInstrumenterCustomizerProviders();
    InternalInstrumenterCustomizerUtil.setInstrumenterCustomizerProviders(
        singletonList(customizer ->
            PreAggregatedMetricsCustomizer.customize(
                new InstrumenterCustomizerImpl(customizer), emitOldRpcSemconv)));

    metricReader = InMemoryMetricReader.create();
    SdkMeterProviderBuilder meterProviderBuilder = SdkMeterProvider.builder();
    AiViewRegistry.registerViews(meterProviderBuilder);
    spanExporter = InMemorySpanExporter.create();
    openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                    .build())
            .setMeterProvider(meterProviderBuilder.registerMetricReader(metricReader).build())
            .build();
  }

  @AfterEach
  void cleanup() {
    InternalInstrumenterCustomizerUtil.setInstrumenterCustomizerProviders(
        originalCustomizerProviders);
    PreAggregatedMetricsCustomizer.setAzureMonitorMetricExporterEnabled(false);
    openTelemetry.close();
  }

  // e.g. otel.metrics.exporter=none or otlp only, the pre-aggregated metrics don't reach
  // Application Insights, so the standard metrics need to be generated from the spans
  @Test
  void notCustomizedWithoutAzureMonitorMetricExporter() {
    useAgentCustomizerEntryPoint();

    runHttpServer();

    assertThat(getSingleSpan().getAttributes().get(AiSemanticAttributes.IS_PRE_AGGREGATED))
        .isNull();
  }

  @Test
  void customizedWithAzureMonitorMetricExporter() {
    useAgentCustomizerEntryPoint();
    PreAggregatedMetricsCustomizer.setAzureMonitorMetricExporterEnabled(true);

    runHttpServer();

    assertThat(getSingleSpan()).hasAttribute(AiSemanticAttributes.IS_PRE_AGGREGATED, true);
  }

  private static void useAgentCustomizerEntryPoint() {
    InternalInstrumenterCustomizerUtil.setInstrumenterCustomizerProviders(
        singletonList(customizer ->
            PreAggregatedMetricsCustomizer.customize(new InstrumenterCustomizerImpl(customizer))));
  }

  private void runHttpServer() {
    Instrumenter<TestRequest, Integer> instrumenter =
        Instrumenter.<TestRequest, Integer>builder(openTelemetry, "test", request -> "GET")
            .addAttributesExtractor(HttpServerAttributesExtractor.create(new HttpGetter()))
            .addOperationMetrics(HttpServerMetrics.get())
            .buildInstrumenter(SpanKindExtractor.alwaysServer());
    TestRequest request = new TestRequest(null);
    Context context = instrumenter.start(Context.root(), request);
    instrumenter.end(context, request, 200, null);
  }

  @Test
  void httpServer() {
    Instrumenter<TestRequest, Integer> instrumenter =
        Instrumenter.<TestRequest, Integer>builder(openTelemetry, "test", request -> "GET")
            .addAttributesExtractor(HttpServerAttributesExtractor.create(new HttpGetter()))
            .addOperationMetrics(HttpServerMetrics.get())
            .buildInstrumenter(SpanKindExtractor.alwaysServer());

    TestRequest request = new TestRequest("Mozilla/5.0 (compatible; AlwaysOn)");
    Context parentContext =
        Context.root()
            .with(AiContextKeys.CONNECTION_STRING, "InstrumentationKey=override")
            .with(AiContextKeys.ROLE_NAME, "role-override");
    Context context = instrumenter.start(parentContext, request);
    instrumenter.end(context, request, 200, null);

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
    assertThat(getSingleSpan()).hasAttribute(AiSemanticAttributes.IS_PRE_AGGREGATED, true);
  }

  @Test
  void rpcClient() {
    Instrumenter<TestRequest, Integer> instrumenter =
        rpcClientInstrumenterBuilder()
            .addOperationMetrics(RpcClientMetrics.get())
            .buildInstrumenter(SpanKindExtractor.alwaysClient());

    runRpcClient(instrumenter);

    assertThat(getMetric("rpc.client.duration"))
        .hasHistogramSatisfying(histogram ->
            histogram.hasPointsSatisfying(point ->
                point.hasAttributesSatisfyingExactly(
                    equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "test-rpc"))));
    assertThat(getSingleSpan()).hasAttribute(AiSemanticAttributes.IS_PRE_AGGREGATED, true);
  }

  // e.g. the RMI and AWS SDK instrumenters
  @Test
  void rpcClientWithoutMetrics() {
    Instrumenter<TestRequest, Integer> instrumenter =
        rpcClientInstrumenterBuilder().buildInstrumenter(SpanKindExtractor.alwaysClient());

    runRpcClient(instrumenter);

    assertThat(metricReader.collectAllMetrics()).isEmpty();
    assertThat(getSingleSpan().getAttributes().get(AiSemanticAttributes.IS_PRE_AGGREGATED))
        .isNull();
  }

  // with otel.semconv-stability.opt-in=rpc, the upstream rpc metrics record rpc.*.call.duration,
  // which are not pre-aggregated standard metrics
  @Test
  void rpcClientIsNotCustomizedWithStableRpcSemconvOnly() {
    emitOldRpcSemconv = false;
    Instrumenter<TestRequest, Integer> instrumenter =
        rpcClientInstrumenterBuilder()
            .addOperationMetrics(RpcClientMetrics.get())
            .buildInstrumenter(SpanKindExtractor.alwaysClient());

    runRpcClient(instrumenter);

    assertThat(getSingleSpan().getAttributes().get(AiSemanticAttributes.IS_PRE_AGGREGATED))
        .isNull();
  }

  private InstrumenterBuilder<TestRequest, Integer> rpcClientInstrumenterBuilder() {
    return Instrumenter.<TestRequest, Integer>builder(openTelemetry, "test", request -> "rpc")
        .addAttributesExtractor(RpcClientAttributesExtractor.create(new RpcGetter()));
  }

  private static void runRpcClient(Instrumenter<TestRequest, Integer> instrumenter) {
    TestRequest request = new TestRequest(null);
    Context context = instrumenter.start(Context.root(), request);
    instrumenter.end(context, request, null, null);
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

  private static class TestRequest {
    @Nullable private final String userAgent;

    private TestRequest(@Nullable String userAgent) {
      this.userAgent = userAgent;
    }
  }

  private static class HttpGetter implements HttpServerAttributesGetter<TestRequest, Integer> {

    @Override
    public String getHttpRequestMethod(TestRequest request) {
      return "GET";
    }

    @Override
    public List<String> getHttpRequestHeader(TestRequest request, String name) {
      if (name.equals("user-agent") && request.userAgent != null) {
        return singletonList(request.userAgent);
      }
      return emptyList();
    }

    @Override
    @Nullable
    public Integer getHttpResponseStatusCode(
        TestRequest request, Integer response, @Nullable Throwable error) {
      return response;
    }

    @Override
    public List<String> getHttpResponseHeader(TestRequest request, Integer response, String name) {
      return emptyList();
    }

    @Override
    public String getUrlScheme(TestRequest request) {
      return "http";
    }

    @Override
    public String getUrlPath(TestRequest request) {
      return "/";
    }

    @Override
    @Nullable
    public String getUrlQuery(TestRequest request) {
      return null;
    }
  }

  @SuppressWarnings("deprecation") // getSystem() and getMethod() are deprecated in newer versions
  private static class RpcGetter implements RpcAttributesGetter<TestRequest, Integer> {

    @Override
    public String getSystem(TestRequest request) {
      return "test-rpc";
    }

    @Override
    public String getService(TestRequest request) {
      return "TestService";
    }

    @Override
    public String getMethod(TestRequest request) {
      return "testMethod";
    }
  }
}
