package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import java.util.Collections;

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

    @Test
    public void testOverlayWithEnvVarWithNone() {
        assertEquals("def", ConfigurationBuilder.overlayWithEnvVars("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithFirst() {
        envVars.set("myenv", "abc");
        assertEquals("abc", ConfigurationBuilder.overlayWithEnvVars("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithSecond() {
        envVars.set("myenv2", "xyz");
        assertEquals("xyz", ConfigurationBuilder.overlayWithEnvVars("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithBoth() {
        envVars.set("myenv", "abc");
        envVars.set("myenv2", "xyz");
        assertEquals("abc", ConfigurationBuilder.overlayWithEnvVars("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithoutMap() {
        assertEquals(Collections.singletonMap("one", "two"),
                ConfigurationBuilder.overlayWithEnvVars("myenv", Collections.singletonMap("one", "two")));
    }

    @Test
    public void testOverlayWithEnvVarWithMap() {
        envVars.set("myenv", "{\"one\": \"2\"}");
        assertEquals(Collections.singletonMap("one", "2"), ConfigurationBuilder.overlayWithEnvVars("myenv",
                Collections.singletonMap("one", "two")));
    }

    @Test
    public void testOverlayWithEnvVarWithMapAndBadType() {
        envVars.set("myenv", "{\"one\": 2}");
        assertEquals(Collections.singletonMap("one", "two"), ConfigurationBuilder.overlayWithEnvVars("myenv",
                Collections.singletonMap("one", "two")));
    }

    @Test
    public void testOverlayWithEnvVarWithMapAndBadJson() {
        envVars.set("myenv", "--some invalid json--");
        assertEquals(Collections.singletonMap("one", "two"), ConfigurationBuilder.overlayWithEnvVars("myenv",
                Collections.singletonMap("one", "two")));
    }
}
