/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.channel.concrete.inprocess;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LimitsEnforcer;
import com.microsoft.applicationinsights.internal.util.Sanitizer;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

import com.google.common.base.Strings;
import com.google.common.base.Preconditions;

/**
 * An implementation of {@link com.microsoft.applicationinsights.channel.TelemetryChannel}
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
    private final static int DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY = 500;
    private final static int MIN_MAX_TELEMETRY_BUFFER_CAPACITY = 1;
    private final static int MAX_MAX_TELEMETRY_BUFFER_CAPACITY = 1000;
    private final static String MAX_MAX_TELEMETRY_BUFFER_CAPACITY_NAME = "MaxTelemetryBufferCapacity";

    private final static int DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 5;
    private final static int MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 1;
    private final static int MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 300;
    private final static String FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME = "FlushIntervalInSeconds";

    private final static String DEVELOPER_MODE_NAME = "DeveloperMode";
    private final static String ENDPOINT_ADDRESS_NAME = "EndpointAddress";
    private final static String MAX_TRANSMISSION_STORAGE_CAPACITY_NAME = "MaxTransmissionStorageFilesCapacityInMB";

    private boolean developerMode = false;
    private static TransmitterFactory s_transmitterFactory;

    private boolean stopped = false;

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
        initialize(endpointAddress,
                   null,
                   developerMode,
                   createDefaultMaxTelemetryBufferCapacityEnforcer(null),
                   createDefaultSendIntervalInSecondsEnforcer(null));
    }

    /**
     * Ctor
     * @param endpointAddress Must be empty string or a valid uri, else an exception will be thrown
     * @param developerMode True will behave in a 'non-production' mode to ease the debugging
     * @param maxTelemetryBufferCapacity Max number of Telemetries we keep in the buffer, when reached we will send the buffer
     *                          Note, value should be between TRANSMIT_BUFFER_MIN_TIMEOUT_IN_MILLIS and TRANSMIT_BUFFER_MAX_TIMEOUT_IN_MILLIS inclusive
     * @param sendIntervalInMillis The maximum number of milliseconds to wait before we send the buffer
     *                          Note, value should be between MIN_MAX_TELEMETRY_BUFFER_CAPACITY and MAX_MAX_TELEMETRY_BUFFER_CAPACITY inclusive
     */
    public InProcessTelemetryChannel(String endpointAddress, boolean developerMode, int maxTelemetryBufferCapacity, int sendIntervalInMillis) {
        initialize(endpointAddress,
                   null,
                   developerMode,
                   createDefaultMaxTelemetryBufferCapacityEnforcer(maxTelemetryBufferCapacity),
                   createDefaultSendIntervalInSecondsEnforcer(sendIntervalInMillis));
    }

    /**
     * This Ctor will query the 'namesAndValues' map for data to initialize itself
     * It will ignore data that is not of its interest, this Ctor is useful for building an instance from configuration
     * @param namesAndValues - The data passed as name and value pairs
     */
    public InProcessTelemetryChannel(Map<String, String> namesAndValues) {
        boolean developerMode = false;
        String endpointAddress = null;

        LimitsEnforcer maxTelemetryBufferCapacityEnforcer = createDefaultMaxTelemetryBufferCapacityEnforcer(null);

        LimitsEnforcer sendIntervalInSecondsEnforcer = createDefaultSendIntervalInSecondsEnforcer(null);

        if (namesAndValues != null) {
            developerMode = Boolean.valueOf(namesAndValues.get(DEVELOPER_MODE_NAME));
            endpointAddress = namesAndValues.get(ENDPOINT_ADDRESS_NAME);

            maxTelemetryBufferCapacityEnforcer.normalizeStringValue(namesAndValues.get(MAX_MAX_TELEMETRY_BUFFER_CAPACITY_NAME));
            sendIntervalInSecondsEnforcer.normalizeStringValue(namesAndValues.get(FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME));
        }

        String maxTransmissionStorageCapacity = namesAndValues.get(MAX_TRANSMISSION_STORAGE_CAPACITY_NAME);
        initialize(endpointAddress, maxTransmissionStorageCapacity, developerMode, maxTelemetryBufferCapacityEnforcer, sendIntervalInSecondsEnforcer);
    }

    /**
     *  Gets value indicating whether this channel is in developer mode.
     */
    @Override
    public boolean isDeveloperMode() {
        return developerMode;
    }

    /**
     * Sets value indicating whether this channel is in developer mode.
     * @param developerMode True or false
     */
    @Override
    public void setDeveloperMode(boolean developerMode) {
        if (developerMode != this.developerMode) {
            this.developerMode = developerMode;
            int maxTelemetriesInBatch = this.developerMode ? 1 : DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY;

            setMaxTelemetriesInBatch(maxTelemetriesInBatch);
        }
    }

    /**
     *  Sends a Telemetry instance through the channel.
     */
    @Override
    public void send(Telemetry telemetry) {
        Preconditions.checkNotNull(telemetry, "Telemetry item must be non null");

        if (isDeveloperMode()) {
            telemetry.getContext().getProperties().put("DeveloperMode", "true");
        }

        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = null;
        try {
            jsonWriter = new JsonTelemetryDataSerializer(writer);
            telemetry.serialize(jsonWriter);
            jsonWriter.close();
            String asJson = writer.toString();
            telemetryBuffer.add(asJson);
        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to serialize Telemetry");
            return;
        }

        if (isDeveloperMode()) {
            writeTelemetryToDebugOutput(telemetry);
        }
    }

    /**
     * Stops on going work
     */
    @Override
    public synchronized void stop(long timeout, TimeUnit timeUnit) {
        try {
            if (stopped) {
                return;
            }

            telemetriesTransmitter.stop(timeout, timeUnit);
            stopped = true;
        } catch (Throwable t) {
        }
    }

    /**
     * Flushes the data that the channel might have internally.
     */
    @Override
    public void flush() {
        telemetryBuffer.flush();
    }

    /**
     * Sets the buffer size
     * @param maxTelemetriesInBatch should be between MIN_MAX_TELEMETRY_BUFFER_CAPACITY
     *                              and MAX_MAX_TELEMETRY_BUFFER_CAPACITY inclusive
     *                              if the number is lower than the minimum then the minimum will be used
     *                              if the number is higher than the maximum then the maximum will be used
     */
    public void setMaxTelemetriesInBatch(int maxTelemetriesInBatch) {
        telemetryBuffer.setMaxTelemetriesInBatch(maxTelemetriesInBatch);
    }

    /**
     * Sets the time tow wait before flushing the internal buffer
     * @param transmitBufferTimeoutInSeconds should be between MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS
     *                                       and MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS inclusive
     *                                       if the number is lower than the minimum then the minimum will be used
     *                                       if the number is higher than the maximum then the maximum will be used
     */
    public void setTransmitBufferTimeoutInSeconds(int transmitBufferTimeoutInSeconds) {
        telemetryBuffer.setTransmitBufferTimeoutInSeconds(transmitBufferTimeoutInSeconds);
    }

    private void writeTelemetryToDebugOutput(Telemetry telemetry) {
        InternalLogger.INSTANCE.trace("InProcessTelemetryChannel sending telemetry");
    }

    private synchronized void initialize(String endpointAddress,
                                         String maxTransmissionStorageCapacity,
                                         boolean developerMode,
                                         LimitsEnforcer maxTelemetryBufferCapacityEnforcer,
                                         LimitsEnforcer sendIntervalInSeconds) {
        makeSureEndpointAddressIsValid(endpointAddress);

        if (s_transmitterFactory == null) {
            s_transmitterFactory = new InProcessTelemetryChannelFactory();
        }

        telemetriesTransmitter = s_transmitterFactory.create(endpointAddress, maxTransmissionStorageCapacity);
        telemetryBuffer = new TelemetryBuffer(telemetriesTransmitter, maxTelemetryBufferCapacityEnforcer, sendIntervalInSeconds);

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
            InternalLogger.INSTANCE.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private LimitsEnforcer createDefaultMaxTelemetryBufferCapacityEnforcer(Integer currentValue) {
        LimitsEnforcer maxItemsInBatchEnforcer =
                LimitsEnforcer.createWithClosestLimitOnError(
                        MAX_MAX_TELEMETRY_BUFFER_CAPACITY_NAME,
                        MIN_MAX_TELEMETRY_BUFFER_CAPACITY,
                        MAX_MAX_TELEMETRY_BUFFER_CAPACITY,
                        DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY,
                        currentValue == null ? DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY : currentValue);

        return maxItemsInBatchEnforcer;
    }

    private LimitsEnforcer createDefaultSendIntervalInSecondsEnforcer(Integer currentValue) {
        LimitsEnforcer sendIntervalInSecondsEnforcer =
                LimitsEnforcer.createWithClosestLimitOnError(
                        FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME,
                        MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS,
                        MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS,
                        DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS,
                        currentValue == null ? DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS : currentValue);

        return sendIntervalInSecondsEnforcer;
    }
}
