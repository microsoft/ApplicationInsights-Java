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

package com.microsoft.applicationinsights.internal.quickpulse;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.util.MockHttpResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class QuickPulseDataFetcherTests {

  @Test
  void testGetCurrentSdkVersion() {
    QuickPulseDataFetcher dataFetcher =
        new QuickPulseDataFetcher(null, new TelemetryClient(), null, null, null);
    String sdkVersion = dataFetcher.getCurrentSdkVersion();
    assertThat(sdkVersion).isNotNull();
    assertThat(sdkVersion).isNotEqualTo("java:unknown");
  }

  @Test
  void endpointIsFormattedCorrectlyWhenUsingConfig() throws URISyntaxException {
    TelemetryClient telemetryClient = new TelemetryClient();
    telemetryClient.setConnectionString("InstrumentationKey=testing-123");
    QuickPulseDataFetcher quickPulseDataFetcher =
        new QuickPulseDataFetcher(null, telemetryClient, null, null, null);
    String quickPulseEndpoint = quickPulseDataFetcher.getQuickPulseEndpoint();
    String endpointUrl = quickPulseDataFetcher.getEndpointUrl(quickPulseEndpoint);
    URI uri = new URI(endpointUrl);
    assertThat(uri).isNotNull();
    assertThat(endpointUrl)
        .isEqualTo(
            "https://rt.services.visualstudio.com/QuickPulseService.svc/post?ikey=testing-123");
  }

  @Test
  void endpointIsFormattedCorrectlyWhenConfigIsNull() throws URISyntaxException {
    QuickPulseDataFetcher quickPulseDataFetcher =
        new QuickPulseDataFetcher(null, new TelemetryClient(), null, null, null);
    String quickPulseEndpoint = quickPulseDataFetcher.getQuickPulseEndpoint();
    String endpointUrl = quickPulseDataFetcher.getEndpointUrl(quickPulseEndpoint);
    URI uri = new URI(endpointUrl);
    assertThat(uri).isNotNull();
    assertThat(endpointUrl)
        .isEqualTo("https://rt.services.visualstudio.com/QuickPulseService.svc/post?ikey=null");
  }

  @Test
  void endpointChangesWithRedirectHeaderAndGetNewPingInterval() {
    Map<String, String> headers = new HashMap<>();
    headers.put("x-ms-qps-service-polling-interval-hint", "1000");
    headers.put("x-ms-qps-service-endpoint-redirect", "https://new.endpoint.com");
    headers.put("x-ms-qps-subscribed", "true");
    HttpHeaders httpHeaders = new HttpHeaders(headers);
    HttpPipeline httpPipeline =
        new HttpPipelineBuilder()
            .httpClient(request -> Mono.just(new MockHttpResponse(request, 200, httpHeaders)))
            .build();
    QuickPulsePingSender quickPulsePingSender =
        new QuickPulsePingSender(
            httpPipeline, new TelemetryClient(), "machine1", "instance1", "qpid123");

    QuickPulseHeaderInfo quickPulseHeaderInfo = quickPulsePingSender.ping(null);
    assertThat(QuickPulseStatus.QP_IS_ON).isEqualTo(quickPulseHeaderInfo.getQuickPulseStatus());
    assertThat(1000).isEqualTo(quickPulseHeaderInfo.getQpsServicePollingInterval());
    assertThat("https://new.endpoint.com")
        .isEqualTo(quickPulseHeaderInfo.getQpsServiceEndpointRedirect());
  }
}
