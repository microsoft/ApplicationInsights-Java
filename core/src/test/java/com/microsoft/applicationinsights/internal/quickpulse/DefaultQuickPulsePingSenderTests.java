package com.microsoft.applicationinsights.internal.quickpulse;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.TelemetryConfigurationTestHelper;
import org.junit.*;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class DefaultQuickPulsePingSenderTests {
    @BeforeClass
    public static void cleanUpActive() {
        TelemetryConfigurationTestHelper.resetActiveTelemetryConfiguration();
    }

    @AfterClass
    public static void cleanUpActiveAgain() {
        TelemetryConfigurationTestHelper.resetActiveTelemetryConfiguration();
    }

    @Test
    public void endpointIsFormattedCorrectlyWhenUsingConfig() {
        final TelemetryConfiguration config = new TelemetryConfiguration();
        config.setConnectionString("InstrumentationKey=testing-123");
        final String endpointUrl = new DefaultQuickPulsePingSender(null, config, null, null).getQuickPulsePingUri();
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
    public void endpointIsFormattedCorrectlyWhenConfigIsNull() {
        final String endpointUrl = new DefaultQuickPulsePingSender(null, (TelemetryConfiguration)null, null, null).getQuickPulsePingUri();
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
