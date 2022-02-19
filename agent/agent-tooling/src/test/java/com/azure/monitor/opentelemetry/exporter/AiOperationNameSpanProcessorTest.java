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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.Configuration;
import com.azure.core.util.FluxUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class AiOperationNameSpanProcessorTest {

  private static Tracer configureAzureMonitorExporter(HttpPipelinePolicy validator) {
    String connectionStringTemplate =
        "InstrumentationKey=ikey;IngestionEndpoint=https://testendpoint.com";
    String connectionString =
        Configuration.getGlobalConfiguration()
            .get("APPLICATIONINSIGHTS_CONNECTION_STRING", connectionStringTemplate);
    AzureMonitorTraceExporter exporter =
        new AzureMonitorExporterBuilder()
            .connectionString(connectionString)
            .addHttpPipelinePolicy(validator)
            .buildTraceExporter();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(new AiOperationNameSpanProcessor())
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();

    OpenTelemetrySdk openTelemetrySdk =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    return openTelemetrySdk.getTracer("Sample");
  }

  @Test
  public void operationNameFromParentTest() throws InterruptedException {
    CountDownLatch exporterCountDown = new CountDownLatch(1);
    final Tracer tracer =
        configureAzureMonitorExporter(
            new ValidationPolicy(exporterCountDown, Arrays.asList("child-span", "myop")));

    Span parentSpan =
        tracer
            .spanBuilder("parent-span")
            .setAttribute(AiOperationNameSpanProcessor.AI_OPERATION_NAME_KEY, "myop")
            .startSpan();
    parentSpan.updateName("parent-span-changed");
    parentSpan.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");

    Context parentContext = Context.current().with(parentSpan);
    try (Scope ignored = parentContext.makeCurrent()) {
      Span childSpan = tracer.spanBuilder("child-span").setParent(parentContext).startSpan();
      childSpan.end();
    } finally {
      parentSpan.end();
    }

    assertTrue(exporterCountDown.await(10, TimeUnit.SECONDS));
  }

  @Test
  @SuppressWarnings("MustBeClosedChecker")
  public void operationNameEmptyFromParentTest() throws InterruptedException {
    CountDownLatch exporterCountDown = new CountDownLatch(1);
    final Tracer tracer =
        configureAzureMonitorExporter(
            new ValidationPolicy(
                exporterCountDown, Arrays.asList("child-span", "POST parent-span-changed")));
    Span parentSpan = tracer.spanBuilder("parent-span").startSpan();
    parentSpan.updateName("parent-span-changed");
    parentSpan.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
    final Scope parentScope = parentSpan.makeCurrent();
    Span span = tracer.spanBuilder("child-span").startSpan();
    final Scope scope = span.makeCurrent();
    try {
      // Thread bound (sync) calls will automatically pick up the parent span and you don't need to
      // pass it explicitly.
    } finally {
      span.end();
      scope.close();
      parentSpan.end();
      parentScope.close();
    }

    assertTrue(exporterCountDown.await(10, TimeUnit.SECONDS));
  }

  @Test
  @SuppressWarnings("MustBeClosedChecker")
  public void operationNameAsSpanNameTest() throws InterruptedException {
    CountDownLatch exporterCountDown = new CountDownLatch(1);
    final Tracer tracer =
        configureAzureMonitorExporter(
            new ValidationPolicy(
                exporterCountDown, Arrays.asList("child-span", "parent-span-changed")));
    createNestedSpan(tracer);
    assertTrue(exporterCountDown.await(10, TimeUnit.SECONDS));
  }

  private static void createNestedSpan(Tracer tracer) {
    Span parentSpan = tracer.spanBuilder("parent").startSpan();
    parentSpan.updateName("parent-span-changed");
    try {
      createChildSpan(tracer, parentSpan);
    } finally {
      parentSpan.end();
    }
  }

  private static void createChildSpan(Tracer tracer, Span parentSpan) {
    Span childSpan =
        tracer.spanBuilder("child").setParent(Context.current().with(parentSpan)).startSpan();
    try {
      // do stuff
    } finally {
      childSpan.end();
    }
  }

  static class ValidationPolicy implements HttpPipelinePolicy {

    private final CountDownLatch countDown;
    private final List<String> expectedValues;

    ValidationPolicy(CountDownLatch countDown, List<String> expectedValues) {
      this.countDown = countDown;
      this.expectedValues = expectedValues;
    }

    @Override
    public Mono<HttpResponse> process(
        HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
      Mono<String> asyncString =
          FluxUtil.collectBytesInByteBufferStream(context.getHttpRequest().getBody())
              .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
      asyncString.subscribe(
          value -> {
            //  System.out.println(value);
            for (String expectedName : expectedValues) {
              if (!value.contains(expectedName)) {
                return;
              }
            }
            countDown.countDown();
          });
      return next.process();
    }
  }
}
