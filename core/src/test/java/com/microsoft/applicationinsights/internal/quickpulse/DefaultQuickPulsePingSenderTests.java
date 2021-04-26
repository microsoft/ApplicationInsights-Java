package com.microsoft.applicationinsights.internal.quickpulse;

import com.microsoft.applicationinsights.TelemetryClient;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.*;
import org.mockito.Mockito;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
    public void endpointChangesWithRedirectHeaderAndGetNewPingInterval() throws IOException {
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        final QuickPulsePingSender quickPulsePingSender = new DefaultQuickPulsePingSender(httpClient, new TelemetryClient(), "machine1",
                "instance1", "role1", "qpid123");

        CloseableHttpResponse response = new BasicCloseableHttpResponse(new BasicStatusLine(new ProtocolVersion("a",1,2), 200, "OK"));
        response.addHeader("x-ms-qps-service-polling-interval-hint", "1000");
        response.addHeader("x-ms-qps-service-endpoint-redirect", "https://new.endpoint.com");
        response.addHeader("x-ms-qps-subscribed", "true");

        Mockito.doReturn(response).when(httpClient).execute((HttpPost) notNull());
        QuickPulseHeaderInfo quickPulseHeaderInfo = quickPulsePingSender.ping(null);

        Assert.assertEquals(quickPulseHeaderInfo.getQuickPulseStatus(), QuickPulseStatus.QP_IS_ON);
        Assert.assertEquals(quickPulseHeaderInfo.getQpsServicePollingInterval(), 1000);
        Assert.assertEquals(quickPulseHeaderInfo.getQpsServiceEndpointRedirect(), "https://new.endpoint.com");
    }


    public static class BasicCloseableHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {

        public BasicCloseableHttpResponse(StatusLine statusline) {
            super(statusline);
        }

        @Override
        public void close() {}
    }
}
