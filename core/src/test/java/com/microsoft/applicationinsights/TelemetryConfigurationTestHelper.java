package com.microsoft.applicationinsights;

public class TelemetryConfigurationTestHelper {
    public static void resetActiveTelemetryConfiguration() {
        TelemetryConfiguration.setActiveAsNull();
    }
}
