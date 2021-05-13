package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.internal.statsbeat.Constants.OperatingSystem;
import com.microsoft.applicationinsights.internal.statsbeat.Constants.ResourceProvider;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CustomDimensionsTest {

    @Test
    public void testResourceProvider() {
        CustomDimensions customDimensions = new CustomDimensions();

        assertEquals(ResourceProvider.UNKNOWN, customDimensions.getResourceProvider());
    }

    @Test
    public void testOperatingSystem() {
        CustomDimensions customDimensions = new CustomDimensions();

        OperatingSystem os = OperatingSystem.OS_UNKNOWN;
        if (SystemInformation.INSTANCE.isWindows()) {
            os = OperatingSystem.OS_WINDOWS;
        } else if (SystemInformation.INSTANCE.isUnix()) {
            os = OperatingSystem.OS_LINUX;
        }
        assertEquals(os, customDimensions.getOperatingSystem());
    }

    @Test
    public void testCustomerIkey() {
        CustomDimensions customDimensions = new CustomDimensions();

        Map<String, String> properties = new HashMap<>();
        customDimensions.populateProperties(properties);

        assertNull(properties.get("cikey"));
    }

    @Test
    public void testVersion() {
        CustomDimensions customDimensions = new CustomDimensions();

        Map<String, String> properties = new HashMap<>();
        customDimensions.populateProperties(properties);

        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
        String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        assertEquals(version, properties.get("version"));
    }

    @Test
    public void testRuntimeVersion() {
        CustomDimensions customDimensions = new CustomDimensions();

        Map<String, String> properties = new HashMap<>();
        customDimensions.populateProperties(properties);

        assertEquals(System.getProperty("java.version"), properties.get("runtimeVersion"));
    }
}
