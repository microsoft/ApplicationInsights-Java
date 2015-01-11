package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * Created by gupele on 12/30/2014.
 */
public interface TelemetryInitializer {
    /// <summary>
    /// Initializes properties of the specified <see cref="ITelemetry"/> object.
    /// </summary>
    void Initialize(Telemetry telemetry);
}
