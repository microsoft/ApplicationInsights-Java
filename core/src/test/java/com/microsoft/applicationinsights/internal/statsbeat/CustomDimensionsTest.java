package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.internal.statsbeat.Constants.OperatingSystem;
import com.microsoft.applicationinsights.internal.statsbeat.Constants.ResourceProvider;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.junit.Test;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_CIKEY;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_OS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_RUNTIME_VERSION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_VERSION;
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

        assertNull(customDimensions.getProperty(CUSTOM_DIMENSIONS_CIKEY));
    }

    @Test
    public void testVersion() {
        CustomDimensions customDimensions = new CustomDimensions();

        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
        String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        assertEquals(version, customDimensions.getProperty(CUSTOM_DIMENSIONS_VERSION));
    }

    @Test
    public void testRuntimeVersion() {
        CustomDimensions customDimensions = new CustomDimensions();

        assertEquals(System.getProperty("java.version"), customDimensions.getProperty(CUSTOM_DIMENSIONS_RUNTIME_VERSION));
    }
}
