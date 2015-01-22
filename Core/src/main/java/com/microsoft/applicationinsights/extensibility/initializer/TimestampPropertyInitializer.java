package com.microsoft.applicationinsights.extensibility.initializer;

import java.util.Date;

import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * An {@link TelemetryInitializer} implementation that sets the timestamp on the {@link com.microsoft.applicationinsights.telemetry.Telemetry}
 * unless the timestamp is already set>.
 */
public final class TimestampPropertyInitializer implements TelemetryInitializer {

    /**
     * Sets the timestamp to 'now' unless the timestamp is already set.
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to initialize.
     */
    @Override
    public void initialize(Telemetry telemetry) {
        if (telemetry.getTimestamp() == null) {
            telemetry.setTimestamp(new Date());
        }
    }
}
