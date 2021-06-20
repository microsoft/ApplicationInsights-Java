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

import static org.junit.jupiter.api.Assertions.*;

class DefaultQuickPulseDataFetcherTests {

    @Test
    void testGetCurrentSdkVersion() {
        DefaultQuickPulseDataFetcher dataFetcher = new DefaultQuickPulseDataFetcher(null, null, null,
                null, null,null);
        String sdkVersion = dataFetcher.getCurrentSdkVersion();
        assertNotNull(sdkVersion);
        assertNotEquals("java:unknown", sdkVersion);
    }

    @Test
    void endpointIsFormattedCorrectlyWhenUsingConfig() {
        final TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setConnectionString("InstrumentationKey=testing-123");
        DefaultQuickPulseDataFetcher defaultQuickPulseDataFetcher = new DefaultQuickPulseDataFetcher(null, telemetryClient, null, null,null,null);
        final String quickPulseEndpoint = defaultQuickPulseDataFetcher.getQuickPulseEndpoint();
        final String endpointUrl = defaultQuickPulseDataFetcher.getEndpointUrl(quickPulseEndpoint);
        try {
            URI uri = new URI(endpointUrl);
            assertNotNull(uri);
            assertEquals("https://rt.services.visualstudio.com/QuickPulseService.svc/post?ikey=testing-123", endpointUrl);
        } catch (URISyntaxException e) {
            fail("Not a valid uri: "+endpointUrl);
        }
    }

    @Test
    void endpointIsFormattedCorrectlyWhenConfigIsNull() {
        DefaultQuickPulseDataFetcher defaultQuickPulseDataFetcher = new DefaultQuickPulseDataFetcher(null, null,
                null,null, null,null);
        final String quickPulseEndpoint = defaultQuickPulseDataFetcher.getQuickPulseEndpoint();
        final String endpointUrl = defaultQuickPulseDataFetcher.getEndpointUrl(quickPulseEndpoint);
        try {
            URI uri = new URI(endpointUrl);
            assertNotNull(uri);
            assertEquals("https://rt.services.visualstudio.com/QuickPulseService.svc/post?ikey=null", endpointUrl);
        } catch (URISyntaxException e) {
            fail("Not a valid uri: "+endpointUrl);
        }
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
        assertEquals(quickPulseHeaderInfo.getQuickPulseStatus(), QuickPulseStatus.QP_IS_ON);
        assertEquals(quickPulseHeaderInfo.getQpsServicePollingInterval(), 1000);
        assertEquals(quickPulseHeaderInfo.getQpsServiceEndpointRedirect(), "https://new.endpoint.com");
    }
}
