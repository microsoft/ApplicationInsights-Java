package com.microsoft.applicationinsights.channel.concrete.inprocess;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Sanitizer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

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

    private final static String DEVELOPER_MODE = "DeveloperMode";
    private final static String EndpointAddress = "EndpointAddress";

    private boolean developerMode = false;

    private TelemetriesTransmitter telemetriesTransmitter;

    private TelemetryBuffer telemetryBuffer;

    public InProcessTelemetryChannel() {
        this(null, false);
    }

    /**
     * Ctor
     * @param endpointAddress Must be empty string or a valid uri, else an exception will be thrown
     * @param developerMode True will behave in a 'non-production' mode to ease the debugging
     */
    public InProcessTelemetryChannel(String endpointAddress, boolean developerMode) {
        initialize(endpointAddress, developerMode);
    }

    /**
     * This ctor will query the 'namesAndValues' map for data to initialize itself
     * It will ignore data that is not of its interest, this ctor is useful for building an instance from configuration
     * @param nameAndValues - The data passed as name and value pairs
     */
    public InProcessTelemetryChannel(Map<String, String> nameAndValues) {
        boolean developerMode = false;
        String endpointAddress = null;

        if (nameAndValues != null) {
            developerMode = Boolean.valueOf(nameAndValues.get(DEVELOPER_MODE));
            endpointAddress = nameAndValues.get(EndpointAddress);
        }

        initialize(endpointAddress, developerMode);
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

        if (isDeveloperMode()) {
            writeTelemetryToDebugOutput(telemetry);
        }
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {

        telemetriesTransmitter.stop(timeout, timeUnit);
    }

    private void writeTelemetryToDebugOutput(Telemetry telemetry) {
        InternalLogger.INSTANCE.log("InProcessTelemetryChannel sending telemetry");
    }

    private void initialize(String endpointAddress, boolean developerMode) {
        makeSureEndpointAddressIsValid(endpointAddress);

        // Temporary
        telemetriesTransmitter = new NoConfigurationTransmitterFactory().create(endpointAddress);
        telemetryBuffer = new TelemetryBuffer(telemetriesTransmitter, DEFAULT_NUMBER_OF_TELEMETRIES_PER_CONTAINER, TRANSMIT_BUFFER_DEFAULT_TIMEOUT_IN_SECONDS);
        setDeveloperMode(developerMode);
    }

    /**
     * The method will throw IllegalArgumentException if the endpointAddress is not a valid uri
     * Please note that a null or empty string is valid as far as the class is concerned and thus considered valid
     * @param endpointAddress
     */
    private void makeSureEndpointAddressIsValid(String endpointAddress) {
        if (Strings.isNullOrEmpty(endpointAddress)) {
            return;
        }

        URI uri = Sanitizer.sanitizeUri(endpointAddress);
        if (uri == null) {
            String errorMessage = String.format("Endpoint address %s is not a valid uri", endpointAddress);
            InternalLogger.INSTANCE.log(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

}
