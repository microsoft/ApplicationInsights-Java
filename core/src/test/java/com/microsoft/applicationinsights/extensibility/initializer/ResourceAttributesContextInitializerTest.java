package com.microsoft.applicationinsights.extensibility.initializer;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class ResourceAttributesContextInitializerTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void firstTest() {
        // setup
        environmentVariables.set("TEST_ENV1", "testVal1");
        environmentVariables.set("TEST_ENV2", "testVal2");

        // populate attributes
        Map<String, String> resourceAttributes = new HashMap<>();
        resourceAttributes.put("test1", "${TEST_ENV1}");
        resourceAttributes.put("test2", "${TEST_ENV1}-${TEST_ENV2}");
        resourceAttributes.put("test3", "prefix-${TEST_ENV1}-suffix");

        ResourceAttributesContextInitializer initializer = new ResourceAttributesContextInitializer(resourceAttributes);
        TelemetryContext context = new TelemetryContext();

        // when
        initializer.initialize(context);

        // verify
        assertEquals("testVal1", context.getProperties().get("test1"));
        assertEquals("testVal1-testVal2", context.getProperties().get("test2"));
        assertEquals("prefix-testVal1-suffix", context.getProperties().get("test3"));
    }
}
