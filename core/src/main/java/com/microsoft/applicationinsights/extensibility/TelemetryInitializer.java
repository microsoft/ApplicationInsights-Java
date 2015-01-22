package com.microsoft.applicationinsights.extensibility;

import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * Created by gupele on 12/30/2014.
 */
public interface TelemetryInitializer {
    /**
     Initializes properties of the specified object.
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to initialize.
     */
    void initialize(Telemetry telemetry);
}
