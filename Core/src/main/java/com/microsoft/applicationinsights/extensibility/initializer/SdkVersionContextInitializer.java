package com.microsoft.applicationinsights.extensibility.initializer;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;

/**
 * Initializer for SDK version.
 */
public class SdkVersionContextInitializer implements ContextInitializer
{
    @Override
    public void Initialize(TelemetryContext context)
    {
        context.getInternal().setSdkVersion("0.12.0.10255");
        context.getInternal().setAgentVersion("0.12.0");
    }
}
