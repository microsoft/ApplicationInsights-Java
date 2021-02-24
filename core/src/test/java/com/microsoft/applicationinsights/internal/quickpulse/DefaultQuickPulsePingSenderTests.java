package com.microsoft.applicationinsights.internal.quickpulse;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.TelemetryConfigurationTestHelper;
import org.junit.*;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class DefaultQuickPulsePingSenderTests {
    @Before
    public void cleanUpActive() {
        TelemetryConfigurationTestHelper.resetActiveTelemetryConfiguration();
    }

    @After
    public void cleanUpActiveAgain() {
        TelemetryConfigurationTestHelper.resetActiveTelemetryConfiguration();
    }

    @Test
    public void endpointIsFormattedCorrectlyWhenUsingConnectionString() {
        final TelemetryConfiguration config = new TelemetryConfiguration();
        config.setConnectionString("InstrumentationKey=testing-123");
        DefaultQuickPulsePingSender defaultQuickPulsePingSender = new DefaultQuickPulsePingSender(null, config, null,null, null,null);
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
        final TelemetryConfiguration config = new TelemetryConfiguration();
        config.setInstrumentationKey("A-test-instrumentation-key");
        DefaultQuickPulsePingSender defaultQuickPulsePingSender = new DefaultQuickPulsePingSender(null, config, null, null,null,null);
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
}
