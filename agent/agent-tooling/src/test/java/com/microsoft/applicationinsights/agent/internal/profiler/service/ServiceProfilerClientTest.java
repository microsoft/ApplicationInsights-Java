// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.test.TestBase;
import com.azure.core.test.TestMode;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceProfilerClientTest extends TestBase {

  @Test
  void getSettings() throws IOException, InterruptedException {
    ServiceProfilerClient serviceProfilerClient = getProfilerFrontendClientV2();

    Instant now = Instant.parse("2022-01-01T00:00:00Z");

    ProfilerConfiguration configuration = serviceProfilerClient.getSettings(Date.from(now)).block();

    assertThat(configuration.getLastModified()).isNotNull();
    assertThat(configuration.isEnabled()).isTrue();
    assertThat(configuration.getCollectionPlan())
        .isEqualTo(
            "--single --mode immediate --immediate-profiling-duration 120"
                + "  --expiration 5249691022697135638 --settings-moniker Portal_REDACTED");
    assertThat(configuration.getCpuTriggerConfiguration())
        .isEqualTo(
            "--cpu-threshold 80 --cpu-trigger-profilingDuration 120"
                + " --cpu-trigger-cooldown 14400 --cpu-trigger-enabled true");
    assertThat(configuration.getMemoryTriggerConfiguration())
        .isEqualTo(
            "--memory-threshold 80 --memory-trigger-profilingDuration 120"
                + " --memory-trigger-cooldown 14400 --memory-trigger-enabled true");
    assertThat(configuration.getDefaultConfiguration()).isNull();
  }

  @Test
  void getUploadAccess() throws IOException {
    ServiceProfilerClient serviceProfilerClient = getProfilerFrontendClientV2();

    UUID id = UUID.randomUUID();
    BlobAccessPass pass = serviceProfilerClient.getUploadAccess(id, "jfr").block();

    System.out.println(pass);

    //    assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.POST);
    //    assertThat(
    //            url.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" +
    // id))
    //        .isTrue();
    //    assertThat(url.contains("action=gettoken")).isTrue();
    //    assertThat(pass.getUriWithSasToken()).isEqualTo("http://localhost:99999");
  }

  @Test
  void reportUploadFinish() throws IOException {
    ServiceProfilerClient serviceProfilerClient = getProfilerFrontendClientV2();

    UUID id = UUID.randomUUID();
    ArtifactAcceptedResponse artifactAcceptedResponse =
        serviceProfilerClient.reportUploadFinish(id, "jfr", "an-etag").block();

    System.out.println(artifactAcceptedResponse);

    //    assertThat(request.getHttpMethod()).isEqualTo(HttpMethod.POST);
    //    assertThat(
    //            url.contains("/api/apps/a-instrumentation-key/artifactkinds/profile/artifacts/" +
    // id))
    //        .isTrue();
    //    assertThat(url.contains("action=commit")).isTrue();
    //    assertThat(artifactAcceptedResponse.getAcceptedTime()).isEqualTo("a-time");
    //    assertThat(artifactAcceptedResponse.getBlobUri()).isEqualTo("a-blob-uri");
    //    assertThat(artifactAcceptedResponse.getCorrelationId()).isEqualTo("a-correlation-id");
    //    assertThat(artifactAcceptedResponse.getStampId()).isEqualTo("a-stamp");
  }

  private ServiceProfilerClient getProfilerFrontendClientV2() throws MalformedURLException {
    return new ServiceProfilerClient(
        new URL("https://agent.azureserviceprofiler.net/"),
        "afbcd285-0b88-4a9b-b6cf-7399d9ed28e8",
        getHttpPipeline());
  }

  private HttpPipeline getHttpPipeline() {
    HttpClient httpClient;
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      httpClient = HttpClient.createDefault();
    } else {
      httpClient = interceptorManager.getPlaybackClient();
    }
    HttpPipeline httpPipeline =
        new HttpPipelineBuilder()
            .httpClient(httpClient)
            .policies(interceptorManager.getRecordPolicy())
            .build();
    return httpPipeline;
  }
}
