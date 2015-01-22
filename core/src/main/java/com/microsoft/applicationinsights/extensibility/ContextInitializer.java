package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;

/**
 * Represents an object that implements supporting logic for TelemetryContext.
 * {@link com.microsoft.applicationinsights.telemetry.TelemetryContext}
 */
public interface ContextInitializer
{
    /**
     * Initializes the given TelemetryContext.
     * @param context A TelemetryContext to initialize.
     */
    void Initialize(TelemetryContext context);
}
