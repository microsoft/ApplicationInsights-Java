// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.test.http.MockHttpResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ServiceProfilerClientTest {

  @Test
  void fallsBackToLegacyFeatureVersionAndCachesResult() throws MalformedURLException {
    List<String> requestUrls = new ArrayList<>();
    HttpClient httpClient =
        request -> {
          requestUrls.add(request.getUrl().toString());
          String body =
              request.getUrl().getQuery().contains("featureVersion=2.0.0")
                  ? "{\"id\":\"00000000-0000-0000-0000-000000000000\",\"enabled\":false}"
                  : settingsJson(true);
          return Mono.just(
              new MockHttpResponse(request, 200, body.getBytes(StandardCharsets.UTF_8)));
        };
    ServiceProfilerClient client = newServiceProfilerClient(httpClient);

    assertThat(client.getSettings(new Date(0)).block().isEnabled()).isTrue();
    assertThat(client.getSettings(new Date(0)).block().isEnabled()).isTrue();

    assertThat(requestUrls)
        .containsExactly(
            "https://agent.azureserviceprofiler.net/api/profileragent/v4/settings?iKey=00000000-0000-0000-0000-000000000000&oldTimestamp=1970-01-01T00:00:00.0Z&featureVersion=2.0.0",
            "https://agent.azureserviceprofiler.net/api/profileragent/v4/settings?iKey=00000000-0000-0000-0000-000000000000&oldTimestamp=1970-01-01T00:00:00.0Z&featureVersion=1.0.0",
            "https://agent.azureserviceprofiler.net/api/profileragent/v4/settings?iKey=00000000-0000-0000-0000-000000000000&oldTimestamp=1970-01-01T00:00:00.0Z&featureVersion=1.0.0");
  }

  @Test
  void keepsTargetedFeatureVersionWhenSupported() throws MalformedURLException {
    List<String> requestUrls = new ArrayList<>();
    HttpClient httpClient =
        request -> {
          requestUrls.add(request.getUrl().toString());
          return Mono.just(
              new MockHttpResponse(
                  request, 200, settingsJson(false).getBytes(StandardCharsets.UTF_8)));
        };
    ServiceProfilerClient client = newServiceProfilerClient(httpClient);

    assertThat(client.getSettings(new Date(0)).block().isEnabled()).isFalse();
    assertThat(client.getSettings(new Date(0)).block().isEnabled()).isFalse();

    assertThat(requestUrls).allMatch(url -> url.contains("featureVersion=2.0.0"));
  }

  private static ServiceProfilerClient newServiceProfilerClient(HttpClient httpClient)
      throws MalformedURLException {
    return new ServiceProfilerClient(
        new URL("https://agent.azureserviceprofiler.net/"),
        "00000000-0000-0000-0000-000000000000",
        new HttpPipelineBuilder().httpClient(httpClient).build());
  }

  private static String settingsJson(boolean enabled) {
    return "{\"id\":\"11111111-1111-1111-1111-111111111111\","
        + "\"lastModified\":\"2026-09-02T16:00:00Z\","
        + "\"enabledLastModified\":\"2026-09-02T16:00:00Z\","
        + "\"enabled\":"
        + enabled
        + "}";
  }
}
