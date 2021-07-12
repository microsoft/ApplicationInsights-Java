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

package com.microsoft.applicationinsights.serviceprofilerapi.upload;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ProfilerFrontendClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.ArtifactAcceptedResponse;
import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.BlobAccessPass;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ProfilerFrontendClientV2Test {
  @Test
  void settingsPullHitsCorrectUrl() throws IOException {

    AtomicReference<HttpRequest> requestHolder = new AtomicReference<>();

    HttpPipeline httpPipeline =
        new HttpPipelineBuilder()
            .httpClient(
                request -> {
                  requestHolder.set(request);
                  return Mono.just(mockResponse(request, 200, "some-settings"));
                })
            .build();

    ProfilerFrontendClientV2 profilerFrontendClientV2 =
        new ProfilerFrontendClientV2(
            new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

    Date now = Date.from(Instant.now());
    String settings = profilerFrontendClientV2.getSettings(now).block();

    HttpRequest request = requestHolder.get();
    String url = request.getUrl().toString();

    assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.GET);
    assertThat(url.contains("a-instrumentation-key")).isTrue();
    assertThat(url.contains("/api/profileragent/v4/settings")).isTrue();
    assertThat(settings).isEqualTo("some-settings");
  }

  @Test
  void uploadHitsCorrectUrl() throws IOException {

    AtomicReference<HttpRequest> requestHolder = new AtomicReference<>();

    HttpPipeline httpPipeline =
        new HttpPipelineBuilder()
            .httpClient(
                request -> {
                  requestHolder.set(request);
                  return Mono.just(mockResponse(request));
                })
            .build();

    ProfilerFrontendClientV2 profilerFrontendClientV2 =
        new ProfilerFrontendClientV2(
            new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

    UUID id = UUID.randomUUID();
    BlobAccessPass pass = profilerFrontendClientV2.getUploadAccess(id).block();

    HttpRequest request = requestHolder.get();
    String url = request.getUrl().toString();

    assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.POST);
    assertThat(
            url.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" + id))
        .isTrue();
    assertThat(url.contains("action=gettoken")).isTrue();
    assertThat(pass.getUriWithSasToken()).isEqualTo("http://localhost:99999");
  }

  private static MockHttpResponse mockResponse(HttpRequest request) {
    return mockResponse(request, 200, "");
  }

  private static MockHttpResponse mockResponse(HttpRequest request, int code, String body) {
    HashMap<String, String> headers = new HashMap<>();
    headers.put("Location", "http://localhost:99999");
    return new MockHttpResponse(
        request, code, new HttpHeaders(headers), body.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void uploadFinishedHitsCorrectUrl() throws IOException {

    AtomicReference<HttpRequest> requestHolder = new AtomicReference<>();

    HttpPipeline httpPipeline =
        new HttpPipelineBuilder()
            .httpClient(
                request -> {
                  requestHolder.set(request);
                  return Mono.just(
                      mockResponse(
                          request,
                          201,
                          "{\"acceptedTime\":\"a-time\",\"blobUri\":\"a-blob-uri\",\"correlationId\":\"a-correlation-id\",\"stampId\":\"a-stamp\"}"));
                })
            .build();

    ProfilerFrontendClientV2 profilerFrontendClientV2 =
        new ProfilerFrontendClientV2(
            new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

    UUID id = UUID.randomUUID();
    ArtifactAcceptedResponse artifactAcceptedResponse =
        profilerFrontendClientV2.reportUploadFinish(id, "an-etag").block();

    HttpRequest request = requestHolder.get();
    String url = request.getUrl().toString();

    assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.POST);
    assertThat(
            url.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" + id))
        .isTrue();
    assertThat(url.contains("action=commit")).isTrue();
    assertThat(artifactAcceptedResponse.getAcceptedTime()).isEqualTo("a-time");
    assertThat(artifactAcceptedResponse.getBlobUri()).isEqualTo("a-blob-uri");
    assertThat(artifactAcceptedResponse.getCorrelationId()).isEqualTo("a-correlation-id");
    assertThat(artifactAcceptedResponse.getStampId()).isEqualTo("a-stamp");
  }
}
