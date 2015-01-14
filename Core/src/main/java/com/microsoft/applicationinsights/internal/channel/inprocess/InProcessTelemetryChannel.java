package com.microsoft.applicationinsights.internal.channel.inprocess;

import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.TelemetryClientConfiguration;

import com.google.common.base.Preconditions;

/**
 * One of the main {@link com.microsoft.applicationinsights.channel.TelemetryChannel}
 *
 * The channel holds two main entities:
 *
 * A buffer for incoming {@link com.microsoft.applicationinsights.telemetry.Telemetry} instances
 * A transmitter
 *
 * The buffer is stores incoming telemetry instances. Every new buffer starts a timer.
 * When the timer expires, or when the buffer is 'full' (whichever happens first), the
 * transmitter will pick up that buffer and will handle its sending to the server. For example,
 * a transmitter will be responsible for compressing, sending and activate a policy in case of failures.
 *
 * The model here is:
 *
 * Use application threads to populate the buffer
 * Use channel's threads to send buffers to the server
 *
 * Created by gupele on 12/17/2014.
 */
public final class InProcessTelemetryChannel implements TelemetryChannel {
    private final static int DEFAULT_NUMBER_OF_TELEMETRIES_PER_CONTAINER = 128;

    private final static int TRANSMIT_BUFFER_DEFAULT_TIMEOUT_IN_SECONDS = 10;

    private boolean developerMode = false;

    private final TelemetriesTransmitter telemetriesTransmitter;

    private final TelemetryBuffer telemetryBuffer;

    public InProcessTelemetryChannel(TelemetryClientConfiguration configuration) {

        // Temporary
        telemetriesTransmitter = new NoConfigurationTransmitterFactory().create(configuration.getEndpoint());
        telemetryBuffer = new TelemetryBuffer(telemetriesTransmitter, DEFAULT_NUMBER_OF_TELEMETRIES_PER_CONTAINER, TRANSMIT_BUFFER_DEFAULT_TIMEOUT_IN_SECONDS);
        setDeveloperMode(configuration.isDeveloperMode());
    }

    @Override
    public boolean isDeveloperMode() {
        return developerMode;
    }

    @Override
    public void setDeveloperMode(boolean developerMode) {
        if (developerMode != this.developerMode) {
            this.developerMode = developerMode;
            int maxTelemetriesInBatch = this.developerMode ? 1 : DEFAULT_NUMBER_OF_TELEMETRIES_PER_CONTAINER;
            telemetryBuffer.setMaxTelemetriesInBatch(maxTelemetriesInBatch);
        }
    }

    @Override
    public void send(Telemetry telemetry) {
        Preconditions.checkNotNull(telemetry, "Telemetry item must be non null");

        if (isDeveloperMode()) {
            telemetry.getContext().getProperties().put("DeveloperMode", "true");
        }

        telemetryBuffer.add(telemetry);

        if (isDeveloperMode())
        {
            writeTelemetryToDebugOutput(telemetry);
        }
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {

        telemetriesTransmitter.stop(timeout, timeUnit);
    }

    private void writeTelemetryToDebugOutput(Telemetry telemetry) {
        // TODO: decide what 'debug' is and then implement
    }
}
