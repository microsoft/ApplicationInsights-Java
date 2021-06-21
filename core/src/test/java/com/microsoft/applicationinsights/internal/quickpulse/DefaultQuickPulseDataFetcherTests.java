package com.microsoft.applicationinsights.internal.quickpulse;

import com.azure.core.http.*;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.util.MockHttpResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultQuickPulseDataFetcherTests {

    @Test
    void testGetCurrentSdkVersion() {
        DefaultQuickPulseDataFetcher dataFetcher = new DefaultQuickPulseDataFetcher(null, new TelemetryClient(), null,
                null, null,null);
        String sdkVersion = dataFetcher.getCurrentSdkVersion();
        assertThat(sdkVersion).isNotNull();
        assertThat(sdkVersion).isNotEqualTo("java:unknown");
    }

    @Test
    void endpointIsFormattedCorrectlyWhenUsingConfig() throws URISyntaxException {
        TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setConnectionString("InstrumentationKey=testing-123");
        DefaultQuickPulseDataFetcher defaultQuickPulseDataFetcher = new DefaultQuickPulseDataFetcher(null, telemetryClient, null, null,null,null);
        String quickPulseEndpoint = defaultQuickPulseDataFetcher.getQuickPulseEndpoint();
        String endpointUrl = defaultQuickPulseDataFetcher.getEndpointUrl(quickPulseEndpoint);
        URI uri = new URI(endpointUrl);
        assertThat(uri).isNotNull();
        assertThat(endpointUrl).isEqualTo("https://rt.services.visualstudio.com/QuickPulseService.svc/post?ikey=testing-123");
    }

    @Test
    void endpointIsFormattedCorrectlyWhenConfigIsNull() throws URISyntaxException {
        DefaultQuickPulseDataFetcher defaultQuickPulseDataFetcher = new DefaultQuickPulseDataFetcher(null, new TelemetryClient(),
                null,null, null,null);
        String quickPulseEndpoint = defaultQuickPulseDataFetcher.getQuickPulseEndpoint();
        String endpointUrl = defaultQuickPulseDataFetcher.getEndpointUrl(quickPulseEndpoint);
        URI uri = new URI(endpointUrl);
        assertThat(uri).isNotNull();
        assertThat(endpointUrl).isEqualTo("https://rt.services.visualstudio.com/QuickPulseService.svc/post?ikey=null");
    }

    @Test
    void endpointChangesWithRedirectHeaderAndGetNewPingInterval() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-ms-qps-service-polling-interval-hint", "1000");
        headers.put("x-ms-qps-service-endpoint-redirect", "https://new.endpoint.com");
        headers.put("x-ms-qps-subscribed", "true");
        HttpHeaders httpHeaders = new HttpHeaders(headers);
        HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(request -> Mono.just(new MockHttpResponse(request, 200, httpHeaders)))
                .build();
        QuickPulsePingSender quickPulsePingSender = new DefaultQuickPulsePingSender(httpPipeline, new TelemetryClient(), "machine1",
                "instance1", "role1", "qpid123");

        QuickPulseHeaderInfo quickPulseHeaderInfo = quickPulsePingSender.ping(null);
        assertThat(QuickPulseStatus.QP_IS_ON).isEqualTo(quickPulseHeaderInfo.getQuickPulseStatus());
        assertThat(1000).isEqualTo(quickPulseHeaderInfo.getQpsServicePollingInterval());
        assertThat("https://new.endpoint.com").isEqualTo(quickPulseHeaderInfo.getQpsServiceEndpointRedirect());
    }
}
