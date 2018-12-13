package com.microsoft.applicationinsights.web.extensibility.initializers;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.*;

import static org.junit.Assert.*;

public class WebAppNameContextInitializerTest {
    private WebAppNameContextInitializer underTest;
    private TelemetryContext context;

    @Before
    public void setup() {
        underTest = new WebAppNameContextInitializer();
        context = new TelemetryContext();
    }

    @After
    public void tearDown() {
        underTest = null;
        context = null;
    }

    @Test
    public void noOpIfNullAppName() {
        underTest.initialize(context);
        assertNull(context.getCloud().getRole());
    }

    @Test
    public void noOpIfEmptyAppName() {
        underTest.initialize(context);
        assertNull(context.getCloud().getRole());
    }

    @Test
    public void nonEmptyAppNameSetCloudContextRole() {
        final String theAppName = "testAppName";
        underTest.setAppName(theAppName);
        underTest.initialize(context);
        assertEquals(theAppName, context.getCloud().getRole());
    }
}
