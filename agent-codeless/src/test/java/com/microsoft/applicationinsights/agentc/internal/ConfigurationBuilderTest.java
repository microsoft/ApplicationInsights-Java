package com.microsoft.applicationinsights.agentc.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationBuilderTest {

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
}
