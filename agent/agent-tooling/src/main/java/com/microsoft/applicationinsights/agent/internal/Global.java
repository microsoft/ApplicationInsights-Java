package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.TelemetryClient;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Global {

    @Nullable
    private static volatile TelemetryClient telemetryClient;

    private static volatile double samplingPercentage = 100;

    // this can be null if agent failed during startup
    @Nullable
    public static TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

    public static double getSamplingPercentage() {
        return samplingPercentage;
    }

    public static void setTelemetryClient(TelemetryClient telemetryClient) {
        Global.telemetryClient = telemetryClient;
    }

    public static void setSamplingPercentage(double samplingPercentage) {
        Global.samplingPercentage = samplingPercentage;
    }
}
