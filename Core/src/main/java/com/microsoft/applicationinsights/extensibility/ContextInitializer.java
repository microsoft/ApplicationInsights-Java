package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.datacontracts.TelemetryContext;

/**
 * Represents an object that implements supporting logic for TelemetryContext.
 * @see com.microsoft.applicationinsights.datacontracts.TelemetryContext
 */
public interface ContextInitializer
{
    /**
     * Initializes the given TelemetryContext.
     * @param context A TelemetryContext to initialize.
     */
    void Initialize(TelemetryContext context);
}
