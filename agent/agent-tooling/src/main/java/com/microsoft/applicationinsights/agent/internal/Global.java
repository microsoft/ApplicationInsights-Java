package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.TelemetryClient;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Global {

    @Nullable
    private static volatile TelemetryClient telemetryClient;

    private static volatile double traceIdBasedSamplingProbability = 1;

    // this can be null if agent failed during startup
    @Nullable
    public static TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

    public static double getTraceIdBasedSamplingProbability() {
        return traceIdBasedSamplingProbability;
    }

    public static void setTelemetryClient(TelemetryClient telemetryClient) {
        Global.telemetryClient = telemetryClient;
    }

    public static void setTraceIdBasedSamplingProbability(double traceIdBasedSamplingProbability) {
        Global.traceIdBasedSamplingProbability = traceIdBasedSamplingProbability;
    }
}
