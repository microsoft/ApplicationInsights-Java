package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_CIKEY;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_OS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_RP;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_RUNTIME_VERSION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_VERSION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.OS_LINUX;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.OS_WINDOWS;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.UNKNOWN;
import static org.junit.Assert.assertEquals;

public class CustomDimensionsTest {

    @Before
    public void setup() {
        CustomDimensions.resetForTest();
    }

    @Test
    public void testResourceProvider() {
        assertEquals(UNKNOWN, CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_RP));
    }

    @Test
    public void testOperatingSystem() {
        String os = "UNKNOWN";
        if (SystemInformation.INSTANCE.isWindows()) {
            os = OS_WINDOWS;
        } else if (SystemInformation.INSTANCE.isUnix()) {
            os = OS_LINUX;
        }

        assertEquals(os, CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_OS));
    }

    @Test
    public void testCustomerIkey() {
        assertEquals(null, CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_CIKEY));
    }

    @Test
    public void testVersion() {
        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
        String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        assertEquals(version, CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_VERSION));
    }

    @Test
    public void testRuntimeVersion() {
        assertEquals(System.getProperty("java.version"), CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_RUNTIME_VERSION));
    }
}
