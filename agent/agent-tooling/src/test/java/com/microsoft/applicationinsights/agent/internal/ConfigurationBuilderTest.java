package com.microsoft.applicationinsights.agent.internal;

import java.util.Collections;

import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class ConfigurationBuilderTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @Test
    public void testIsNullOrEmpty() {
        assertTrue(ConfigurationBuilder.isNullOrEmpty("   "));
        assertTrue(ConfigurationBuilder.isNullOrEmpty(""));
        assertTrue(ConfigurationBuilder.isNullOrEmpty(null));
        assertFalse(ConfigurationBuilder.isNullOrEmpty("a"));
        assertFalse(ConfigurationBuilder.isNullOrEmpty("  a  "));
        assertFalse(ConfigurationBuilder.isNullOrEmpty("\t"));
    }

    @Test
    public void testEnvVarName() {
        assertEquals("ABCXYZ", ConfigurationBuilder.getEnvVarName("abcxyz"));
        assertEquals("ABC_XYZ", ConfigurationBuilder.getEnvVarName("abcXyz"));
        assertEquals("ABC_XYZ", ConfigurationBuilder.getEnvVarName("abc.xyz"));
        assertEquals("ABCXYZW3C", ConfigurationBuilder.getEnvVarName("abcxyzw3c"));
        assertEquals("ABC_XYZ_W3C", ConfigurationBuilder.getEnvVarName("abcXyzW3c"));
        assertEquals("ABC_XYZ_W3C", ConfigurationBuilder.getEnvVarName("abc.xyzW3c"));
        assertEquals("ABC_XYZ_W3C", ConfigurationBuilder.getEnvVarName("abc.xyz.w3c"));
    }

    @Test
    public void testOverlayWithEnvVarWithNone() {
        assertEquals("def", ConfigurationBuilder.overlayWithEnvVar("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithFirst() {
        envVars.set("myenv", "abc");
        assertEquals("abc", ConfigurationBuilder.overlayWithEnvVar("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithSecond() {
        envVars.set("myenv2", "xyz");
        assertEquals("xyz", ConfigurationBuilder.overlayWithEnvVar("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithBoth() {
        envVars.set("myenv", "abc");
        envVars.set("myenv2", "xyz");
        assertEquals("abc", ConfigurationBuilder.overlayWithEnvVar("myenv", "myenv2", "def"));
    }

    @Test
    public void testOverlayWithEnvVarWithoutMap() {
        assertEquals(Collections.singletonMap("one", "two"),
                ConfigurationBuilder.overlayWithEnvVar("myenv", Collections.singletonMap("one", "two")));
    }

    @Test
    public void testOverlayWithEnvVarWithMap() {
        envVars.set("myenv", "{\"one\": \"2\"}");
        assertEquals(Collections.singletonMap("one", "2"), ConfigurationBuilder.overlayWithEnvVar("myenv",
                Collections.singletonMap("one", "two")));
    }

    @Test
    public void testOverlayWithEnvVarWithMapAndBadType() {
        envVars.set("myenv", "{\"one\": 2}");
        assertEquals(Collections.singletonMap("one", "two"), ConfigurationBuilder.overlayWithEnvVar("myenv",
                Collections.singletonMap("one", "two")));
    }

    @Test
    public void testOverlayWithEnvVarWithMapAndBadJson() {
        envVars.set("myenv", "--some invalid json--");
        assertEquals(Collections.singletonMap("one", "two"), ConfigurationBuilder.overlayWithEnvVar("myenv",
                Collections.singletonMap("one", "two")));
    }
}
