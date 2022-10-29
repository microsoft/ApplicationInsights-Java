// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.upload;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClient;
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

class ServiceProfilerClientTest {
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

    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

    Date now = Date.from(Instant.now());
    String settings = serviceProfilerClient.getSettings(now).block();

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

    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

    UUID id = UUID.randomUUID();
    BlobAccessPass pass = serviceProfilerClient.getUploadAccess(id, "jfr").block();

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

    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(new URL("http://a-host"), "a-instrumentation-key", httpPipeline);

    UUID id = UUID.randomUUID();
    ArtifactAcceptedResponse artifactAcceptedResponse =
        serviceProfilerClient.reportUploadFinish(id, "jfr", "an-etag").block();

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
