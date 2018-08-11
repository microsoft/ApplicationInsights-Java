package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.concurrent.TimeUnit;

public class LocalForwarderTelemetryChannel implements TelemetryChannel {
    @Override
    public boolean isDeveloperMode() {
        return false;
    }

    @Override
    public void setDeveloperMode(boolean value) {

    }

    @Override
    public void send(Telemetry item) {

    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void setSampler(TelemetrySampler telemetrySampler) {

    }
}
