package com.microsoft.applicationinsights.internal.quickpulse;

import com.azure.core.http.*;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.util.MockHttpResponse;
import org.junit.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.*;

public class DefaultQuickPulsePingSenderTests {

    @Test
    public void endpointIsFormattedCorrectlyWhenUsingConnectionString() {
        final TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setConnectionString("InstrumentationKey=testing-123");
        DefaultQuickPulsePingSender defaultQuickPulsePingSender = new DefaultQuickPulsePingSender(null, telemetryClient, null,null, null,null);
        final String quickPulseEndpoint = defaultQuickPulsePingSender.getQuickPulseEndpoint();
        final String endpointUrl = defaultQuickPulsePingSender.getQuickPulsePingUri(quickPulseEndpoint);
        try {
            URI uri = new URI(endpointUrl);
            assertNotNull(uri);
            assertThat(endpointUrl, endsWith("/ping?ikey=testing-123"));
            assertEquals("https://rt.services.visualstudio.com/QuickPulseService.svc/ping?ikey=testing-123", endpointUrl);

        } catch (URISyntaxException e) {
            fail("Not a valid uri: "+endpointUrl);
        }
    }

    @Test
    public void endpointIsFormattedCorrectlyWhenUsingInstrumentationKey() {
        final TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setInstrumentationKey("A-test-instrumentation-key");
        DefaultQuickPulsePingSender defaultQuickPulsePingSender = new DefaultQuickPulsePingSender(null, telemetryClient, null, null,null,null);
        final String quickPulseEndpoint = defaultQuickPulsePingSender.getQuickPulseEndpoint();
        final String endpointUrl = defaultQuickPulsePingSender.getQuickPulsePingUri(quickPulseEndpoint);
        try {
            URI uri = new URI(endpointUrl);
            assertNotNull(uri);
            assertThat(endpointUrl, endsWith("/ping?ikey=A-test-instrumentation-key")); // from resources/ApplicationInsights.xml
            assertEquals("https://rt.services.visualstudio.com/QuickPulseService.svc/ping?ikey=A-test-instrumentation-key", endpointUrl);

        } catch (URISyntaxException e) {
            fail("Not a valid uri: "+endpointUrl);
        }
    }


    @Test
    public void endpointChangesWithRedirectHeaderAndGetNewPingInterval() {
        Map<String, String> headers = new HashMap();
        headers.put("x-ms-qps-service-polling-interval-hint", "1000");
        headers.put("x-ms-qps-service-endpoint-redirect", "https://new.endpoint.com");
        headers.put("x-ms-qps-subscribed", "true");
        HttpHeaders httpHeaders = new HttpHeaders(headers);
        final HttpPipeline httpPipeline = new HttpPipelineBuilder()
                .httpClient(request -> Mono.just(new MockHttpResponse(request, 200, httpHeaders)))
                .build();
        final QuickPulsePingSender quickPulsePingSender = new DefaultQuickPulsePingSender(httpPipeline, new TelemetryClient(), "machine1",
                "instance1", "role1", "qpid123");
        QuickPulseHeaderInfo quickPulseHeaderInfo = quickPulsePingSender.ping(null);
        Assert.assertEquals(quickPulseHeaderInfo.getQuickPulseStatus(), QuickPulseStatus.QP_IS_ON);
        Assert.assertEquals(quickPulseHeaderInfo.getQpsServicePollingInterval(), 1000);
        Assert.assertEquals(quickPulseHeaderInfo.getQpsServiceEndpointRedirect(), "https://new.endpoint.com");
    }
}
