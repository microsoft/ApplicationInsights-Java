package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static com.microsoft.applicationinsights.agent.bootstrap.configuration.ConfigurationBuilder.trimAndEmptyToNull;
import static org.junit.Assert.*;

public class ConfigurationBuilderTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @Test
    public void testEmptyToNull() {
        assertEquals(null, trimAndEmptyToNull("   "));
        assertEquals(null, trimAndEmptyToNull(""));
        assertEquals(null, trimAndEmptyToNull(null));
        assertEquals("a", trimAndEmptyToNull("a"));
        assertEquals("a", trimAndEmptyToNull("  a  "));
        assertEquals(null, trimAndEmptyToNull("\t"));
    }
}
