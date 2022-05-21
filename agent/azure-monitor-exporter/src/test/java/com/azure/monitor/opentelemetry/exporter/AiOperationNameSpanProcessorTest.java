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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.Configuration;
import com.azure.core.util.FluxUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class AiOperationNameSpanProcessorTest {

  private static Tracer configureAzureMonitorExporter(HttpPipelinePolicy validator) {
    String connectionStringTemplate =
        "InstrumentationKey=ikey;IngestionEndpoint=https://test.applicationinsights.azure.com/";
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
    Tracer tracer =
        configureAzureMonitorExporter(
            new ValidationPolicy(exporterCountDown, Arrays.asList("child-span", "myop")));

    Span parentSpan =
        tracer
            .spanBuilder("parent-span")
            .setAttribute(AiOperationNameSpanProcessor.AI_OPERATION_NAME_KEY, "myop")
            .startSpan();
    parentSpan.updateName("parent-span-changed");
    parentSpan.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
    try (Scope parentScope = parentSpan.makeCurrent()) {
      Span childSpan = tracer.spanBuilder("child-span").startSpan();
      try (Scope childScope = childSpan.makeCurrent()) {
        // Thread bound (sync) calls will automatically pick up the parent span and you don't need
        // to
        // pass it explicitly.
      } finally {
        childSpan.end();
      }
    } finally {
      parentSpan.end();
    }
    assertTrue(exporterCountDown.await(10, TimeUnit.SECONDS));
  }

  @Test
  public void operationNameEmptyFromParentTest() throws InterruptedException {
    CountDownLatch exporterCountDown = new CountDownLatch(1);
    Tracer tracer =
        configureAzureMonitorExporter(
            new ValidationPolicy(
                exporterCountDown, Arrays.asList("child-span", "POST /parent-span-changed")));
    Span parentSpan = tracer.spanBuilder("parent-span").startSpan();
    parentSpan.updateName("/parent-span-changed");
    parentSpan.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
    try (Scope parentScope = parentSpan.makeCurrent()) {
      Span childSpan = tracer.spanBuilder("child-span").startSpan();
      try (Scope childScope = childSpan.makeCurrent()) {
        // Thread bound (sync) calls will automatically pick up the parent span and you don't need
        // to
        // pass it explicitly.
      } finally {
        childSpan.end();
      }
    } finally {
      parentSpan.end();
    }
    assertTrue(exporterCountDown.await(10, TimeUnit.SECONDS));
  }

  @Test
  public void operationNameAsSpanNameTest() throws InterruptedException {
    CountDownLatch exporterCountDown = new CountDownLatch(1);
    Tracer tracer =
        configureAzureMonitorExporter(
            new ValidationPolicy(
                exporterCountDown, Arrays.asList("child-span", "parent-span-changed")));
    Span parentSpan = tracer.spanBuilder("parent-span").startSpan();
    parentSpan.updateName("parent-span-changed");
    try (Scope parentScope = parentSpan.makeCurrent()) {
      Span childSpan = tracer.spanBuilder("child-span").startSpan();
      try (Scope childScope = childSpan.makeCurrent()) {
        // Thread bound (sync) calls will automatically pick up the parent span and you don't need
        // to
        // pass it explicitly.
      } finally {
        childSpan.end();
      }
    } finally {
      parentSpan.end();
    }
    assertTrue(exporterCountDown.await(10, TimeUnit.SECONDS));
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
              .map(
                  bytes -> {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                    byte[] ungzip = new byte[bytes.length * 3];
                    int read = 0;
                    try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
                      read = gzipInputStream.read(ungzip, 0, ungzip.length);
                    } catch (IOException e) {
                      // e.printStackTrace();
                    } finally {
                      try {
                        inputStream.close();
                      } catch (IOException e) {
                        // e.printStackTrace();
                      }
                    }
                    return new String(Arrays.copyOf(ungzip, read), UTF_8);
                  });
      asyncString.subscribe(
          value -> {
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
