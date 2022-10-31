// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.test.TestBase;
import com.azure.core.test.TestMode;
import com.fasterxml.jackson.core.JsonParseException;
import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ConfigServiceTest extends TestBase {

  @Test
  void pullSettings() throws IOException, InterruptedException {
    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            new URL("https://agent.azureserviceprofiler.net/"),
            "00000000-0000-0000-0000-000000000000",
            getHttpPipeline());

    ConfigService configService = new ConfigService(serviceProfilerClient);

    ProfilerConfiguration configuration = configService.pullSettings().block();

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
  void badServiceResponseDoesNotProvideReturn() throws MalformedURLException {
    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            new URL("https://agent.azureserviceprofiler.net/"),
            "00000000-0000-0000-0000-000000000000",
            getHttpPipeline());

    ConfigService configService = new ConfigService(serviceProfilerClient);
    Mono<ProfilerConfiguration> result = configService.pullSettings();

    assertThatThrownBy(result::block).hasRootCauseInstanceOf(JsonParseException.class);
  }

  private HttpPipeline getHttpPipeline() {
    if (getTestMode() == TestMode.RECORD || getTestMode() == TestMode.LIVE) {
      return new HttpPipelineBuilder()
          .httpClient(HttpClient.createDefault())
          .policies(interceptorManager.getRecordPolicy())
          .build();
    } else {
      return new HttpPipelineBuilder().httpClient(interceptorManager.getPlaybackClient()).build();
    }
  }
}
