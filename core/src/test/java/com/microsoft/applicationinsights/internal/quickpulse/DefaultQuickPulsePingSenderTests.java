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

class DefaultQuickPulsePingSenderTests {

    @Test
    void endpointIsFormattedCorrectlyWhenUsingConnectionString() throws URISyntaxException {
        final TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setConnectionString("InstrumentationKey=testing-123");
        DefaultQuickPulsePingSender defaultQuickPulsePingSender = new DefaultQuickPulsePingSender(null, telemetryClient, null,null, null,null);
        final String quickPulseEndpoint = defaultQuickPulsePingSender.getQuickPulseEndpoint();
        final String endpointUrl = defaultQuickPulsePingSender.getQuickPulsePingUri(quickPulseEndpoint);
        URI uri = new URI(endpointUrl);
        assertThat(uri).isNotNull();
        assertThat(endpointUrl).endsWith("/ping?ikey=testing-123");
        assertThat(endpointUrl).isEqualTo("https://rt.services.visualstudio.com/QuickPulseService.svc/ping?ikey=testing-123");
    }

    @Test
    void endpointIsFormattedCorrectlyWhenUsingInstrumentationKey() throws URISyntaxException {
        final TelemetryClient telemetryClient = new TelemetryClient();
        telemetryClient.setInstrumentationKey("A-test-instrumentation-key");
        DefaultQuickPulsePingSender defaultQuickPulsePingSender = new DefaultQuickPulsePingSender(null, telemetryClient, null, null,null,null);
        final String quickPulseEndpoint = defaultQuickPulsePingSender.getQuickPulseEndpoint();
        final String endpointUrl = defaultQuickPulsePingSender.getQuickPulsePingUri(quickPulseEndpoint);
        URI uri = new URI(endpointUrl);
        assertThat(uri).isNotNull();
        assertThat(endpointUrl).endsWith("/ping?ikey=A-test-instrumentation-key"); // from resources/ApplicationInsights.xml
        assertThat(endpointUrl).isEqualTo("https://rt.services.visualstudio.com/QuickPulseService.svc/ping?ikey=A-test-instrumentation-key");
    }

    @Test
    void endpointChangesWithRedirectHeaderAndGetNewPingInterval() {
        Map<String, String> headers = new HashMap<>();
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
        assertThat(QuickPulseStatus.QP_IS_ON).isEqualTo(quickPulseHeaderInfo.getQuickPulseStatus());
        assertThat(1000).isEqualTo(quickPulseHeaderInfo.getQpsServicePollingInterval());
        assertThat("https://new.endpoint.com").isEqualTo(quickPulseHeaderInfo.getQpsServiceEndpointRedirect());
    }
}
