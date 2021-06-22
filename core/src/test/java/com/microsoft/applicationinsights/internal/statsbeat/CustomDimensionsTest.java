package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomDimensionsTest {

    @Test
    public void testResourceProvider() {
        CustomDimensions customDimensions = new CustomDimensions();

        assertThat(customDimensions.getResourceProvider()).isEqualTo(ResourceProvider.UNKNOWN);
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
        assertThat(customDimensions.getOperatingSystem()).isEqualTo(os);
    }

    @Test
    public void testCustomerIkey() {
        CustomDimensions customDimensions = new CustomDimensions();

        Map<String, String> properties = new HashMap<>();
        customDimensions.populateProperties(properties, null);

        assertThat(properties.get("cikey")).isNull();
    }

    @Test
    public void testVersion() {
        CustomDimensions customDimensions = new CustomDimensions();

        Map<String, String> properties = new HashMap<>();
        customDimensions.populateProperties(properties, null);

        String sdkVersion = PropertyHelper.getQualifiedSdkVersionString();
        String version = sdkVersion.substring(sdkVersion.lastIndexOf(':') + 1);
        assertThat(properties.get("version")).isEqualTo(version);
    }

    @Test
    public void testRuntimeVersion() {
        CustomDimensions customDimensions = new CustomDimensions();

        Map<String, String> properties = new HashMap<>();
        customDimensions.populateProperties(properties, null);

        assertThat(properties.get("runtimeVersion")).isEqualTo(System.getProperty("java.version"));
    }
}
