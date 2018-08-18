package com.microsoft.applicationinsights.channel.concrete.nop;

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.concurrent.TimeUnit;

/**
 * Null-object implementation of TelemetryChannel. Used when a custom channel is misconfigured.
 */
public class NopTelemetryChannel implements TelemetryChannel {
    @Override
    public boolean isDeveloperMode() {
        // doesn't matter
        return false;
    }

    @Override
    public void setDeveloperMode(boolean value) {
        // nop
    }

    @Override
    public void send(Telemetry item) {
        // nop
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        // nop
    }

    @Override
    public void flush() {
        // nop
    }

    @Override
    public void setSampler(TelemetrySampler telemetrySampler) {
        // nop
    }
}
