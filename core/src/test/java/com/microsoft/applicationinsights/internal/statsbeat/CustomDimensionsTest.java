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
        CustomDimensions.reset();
    }

    @Test
    public void testResourceProvider() {
        System.out.println("#### " + CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_RP));
        assertEquals(CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_RP), UNKNOWN);
    }

    @Test
    public void testOperatingSystem() {
        String os = "UNKNOWN";
        if (SystemInformation.INSTANCE.isWindows()) {
            os = OS_WINDOWS;
        } else if (SystemInformation.INSTANCE.isUnix()) {
            os = OS_LINUX;
        }

        assertEquals(CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_OS), os);
    }

    @Test
    public void testCustomerIkey() {
        assertEquals(CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_CIKEY), null);
    }

    @Test
    public void testVersion() {
        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
        String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        assertEquals(CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_VERSION), version);
    }

    @Test
    public void testRuntimeVersion() {
        assertEquals(CustomDimensions.getInstance().getProperties().get(CUSTOM_DIMENSIONS_RUNTIME_VERSION), System.getProperty("java.version"));
    }
}
