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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.test.http.MockHttpResponse;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class AzureMonitorRedirectPolicyTest {

  @Test
  public void retryWith308Test() throws Exception {
    RecordingHttpClient httpClient =
        new RecordingHttpClient(
            request -> {
              if (request.getUrl().toString().equals("http://localhost/")) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Location", "http://redirecthost/");
                HttpHeaders httpHeader = new HttpHeaders(headers);
                return Mono.just(new MockHttpResponse(request, 308, httpHeader));
              } else {
                return Mono.just(new MockHttpResponse(request, 200));
              }
            });

    HttpPipeline pipeline =
        new HttpPipelineBuilder()
            .httpClient(httpClient)
            .policies(new AzureMonitorRedirectPolicy())
            .build();

    HttpResponse response =
        pipeline.send(new HttpRequest(HttpMethod.GET, new URL("http://localhost/"))).block();

    assertEquals(2, httpClient.getCount());
    assertEquals(200, response.getStatusCode());
  }

  @Test
  public void retryMaxTest() throws Exception {
    RecordingHttpClient httpClient =
        new RecordingHttpClient(
            request -> {
              Map<String, String> headers = new HashMap<>();
              headers.put("Location", "http://redirecthost/");
              HttpHeaders httpHeader = new HttpHeaders(headers);
              return Mono.just(new MockHttpResponse(request, 308, httpHeader));
            });

    HttpPipeline pipeline =
        new HttpPipelineBuilder()
            .httpClient(httpClient)
            .policies(new AzureMonitorRedirectPolicy())
            .build();

    HttpResponse response =
        pipeline.send(new HttpRequest(HttpMethod.GET, new URL("http://localhost/"))).block();
    // redirect is captured only 3 times
    assertEquals(11, httpClient.getCount());
    assertEquals(308, response.getStatusCode());
  }

  @Test
  public void retryWith308MultipleRequestsTest() throws Exception {
    RecordingHttpClient httpClient =
        new RecordingHttpClient(
            request -> {
              if (request.getUrl().toString().equals("http://localhost/")) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Location", "http://redirecthost/");
                HttpHeaders httpHeader = new HttpHeaders(headers);
                return Mono.just(new MockHttpResponse(request, 308, httpHeader));
              } else {
                return Mono.just(new MockHttpResponse(request, 200));
              }
            });

    HttpPipeline pipeline =
        new HttpPipelineBuilder()
            .httpClient(httpClient)
            .policies(new AzureMonitorRedirectPolicy())
            .build();

    assertEquals(0, httpClient.getCount());
    HttpResponse response1 =
        pipeline.send(new HttpRequest(HttpMethod.GET, new URL("http://localhost/"))).block();
    assertEquals(200, response1.getStatusCode());
    assertEquals(2, httpClient.getCount());

    httpClient.resetCount();
    HttpResponse response2 =
        pipeline.send(new HttpRequest(HttpMethod.GET, new URL("http://localhost/"))).block();
    assertEquals(200, response2.getStatusCode());
    // Make sure the future requests are sent directly to http://redirecthost/
    assertEquals(1, httpClient.getCount());
  }

  static class RecordingHttpClient implements HttpClient {

    private final AtomicInteger count = new AtomicInteger();
    private final Function<HttpRequest, Mono<HttpResponse>> handler;

    RecordingHttpClient(Function<HttpRequest, Mono<HttpResponse>> handler) {
      this.handler = handler;
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest httpRequest) {
      count.getAndIncrement();
      return handler.apply(httpRequest);
    }

    int getCount() {
      return count.get();
    }

    void resetCount() {
      count.set(0);
    }
  }
}
