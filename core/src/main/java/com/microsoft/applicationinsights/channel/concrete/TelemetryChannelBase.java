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

package com.microsoft.applicationinsights.channel.concrete;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LimitsEnforcer;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.Sanitizer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @param <T> The type of the telemetry being stored in the buffer.
 */
public abstract class TelemetryChannelBase<T> implements TelemetryChannel {
    public static final int DEFAULT_MAX_INSTANT_RETRY = 3;
    public static final int DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY = 500;
    public static final int DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 5;
    public static final int MIN_MAX_TELEMETRY_BUFFER_CAPACITY = 1;
    public static final int MAX_MAX_TELEMETRY_BUFFER_CAPACITY = 1000;
    public static final int MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 1;
    public static final int MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 300;
    public static final String DEVELOPER_MODE_SYSTEM_PROPRETY_NAME = "APPLICATION_INSIGHTS_DEVELOPER_MODE";

    public static final String MAX_TELEMETRY_BUFFER_CAPACITY_NAME = "MaxTelemetryBufferCapacity";
    public static final String INSTANT_RETRY_NAME = "MaxInstantRetry";
    public static final String FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME = "FlushIntervalInSeconds";
    public static final String DEVELOPER_MODE_NAME = "DeveloperMode";
    public static final String ENDPOINT_ADDRESS_NAME = "EndpointAddress";
    public static final String MAX_TRANSMISSION_STORAGE_CAPACITY_NAME = "MaxTransmissionStorageFilesCapacityInMB";
    public static final int LOG_TELEMETRY_ITEMS_MODULUS = 10000;
    public static final String THROTTLING_ENABLED_NAME = "Throttling";

    private TransmitterFactory transmitterFactory;
    private AtomicLong itemsSent = new AtomicLong(0);

    protected boolean stopped = false;

    protected TelemetriesTransmitter<T> telemetriesTransmitter;
    protected TelemetrySampler telemetrySampler;
    protected TelemetryBuffer<T> telemetryBuffer;

    private boolean developerMode = false;

    public TelemetryChannelBase() {
        boolean developerMode = false;
        try {
            String developerModeAsString = System.getProperty(DEVELOPER_MODE_SYSTEM_PROPRETY_NAME);
            if (!LocalStringsUtils.isNullOrEmpty(developerModeAsString)) {
                developerMode = Boolean.valueOf(developerModeAsString);
            }
        } catch (Exception e) {
            developerMode = false;
            InternalLogger.INSTANCE.trace("%s generated exception in parsing, stack trace is %s", DEVELOPER_MODE_SYSTEM_PROPRETY_NAME, ExceptionUtils.getStackTrace(e));
        }
        initialize(
                null,
                null,
                developerMode,
                createDefaultMaxTelemetryBufferCapacityEnforcer(null),
                createDefaultSendIntervalInSecondsEnforcer(null),
                true,
                DEFAULT_MAX_INSTANT_RETRY);
    }

    /**
     * Ctor
     *
     * @param endpointAddress Must be empty string or a valid uri, else an exception will be thrown
     * @param developerMode True will behave in a 'non-production' mode to ease the debugging
     * @param maxTelemetryBufferCapacity Max number of Telemetries we keep in the buffer, when reached
     *     we will send the buffer Note, value should be between TRANSMIT_BUFFER_MIN_TIMEOUT_IN_MILLIS
     *     and TRANSMIT_BUFFER_MAX_TIMEOUT_IN_MILLIS inclusive
     * @param sendIntervalInMillis The maximum number of milliseconds to wait before we send the
     *     buffer Note, value should be between MIN_MAX_TELEMETRY_BUFFER_CAPACITY and
     *     MAX_MAX_TELEMETRY_BUFFER_CAPACITY inclusive
     */
    public TelemetryChannelBase(String endpointAddress, boolean developerMode, int maxTelemetryBufferCapacity, int sendIntervalInMillis) {
        this(
                endpointAddress,
                null,
                developerMode,
                maxTelemetryBufferCapacity,
                sendIntervalInMillis,
                true,
                DEFAULT_MAX_INSTANT_RETRY);
    }

    public TelemetryChannelBase(
            String endpointAddress,
            String maxTransmissionStorageCapacity,
            boolean developerMode,
            int maxTelemetryBufferCapacity,
            int sendIntervalInMillis,
            boolean throttling,
            int maxInstantRetries) {
        initialize(
                endpointAddress,
                maxTransmissionStorageCapacity,
                developerMode,
                createDefaultMaxTelemetryBufferCapacityEnforcer(maxTelemetryBufferCapacity),
                createDefaultSendIntervalInSecondsEnforcer(sendIntervalInMillis),
                throttling,
                maxInstantRetries);
    }

    /**
     * This Ctor will query the 'namesAndValues' map for data to initialize itself
     * It will ignore data that is not of its interest, this Ctor is useful for
     * building an instance from configuration
     *
     * @param namesAndValues - The data passed as name and value pairs
     */
    public TelemetryChannelBase(Map<String, String> namesAndValues) {
        boolean developerMode = false;
        String endpointAddress = null;
        int maxInstantRetries = DEFAULT_MAX_INSTANT_RETRY;

        LimitsEnforcer maxTelemetryBufferCapacityEnforcer = createDefaultMaxTelemetryBufferCapacityEnforcer(null);

        LimitsEnforcer sendIntervalInSecondsEnforcer = createDefaultSendIntervalInSecondsEnforcer(null);

        boolean throttling = true;
        if (namesAndValues != null) {
            throttling = Boolean.valueOf(namesAndValues.get(THROTTLING_ENABLED_NAME));
            developerMode = Boolean.valueOf(namesAndValues.get(DEVELOPER_MODE_NAME));
            try {
                String instantRetryValue = namesAndValues.get(INSTANT_RETRY_NAME);
                if (instantRetryValue != null) {
                    maxInstantRetries = Integer.parseInt(instantRetryValue);
                }

            } catch (NumberFormatException e) {
                InternalLogger.INSTANCE.error("Unable to parse configuration setting %s to integer value.%nStack Trace:%n%s", INSTANT_RETRY_NAME, ExceptionUtils.getStackTrace(e));
            }

            if (!developerMode) {
                developerMode = Boolean.valueOf(System.getProperty(DEVELOPER_MODE_SYSTEM_PROPRETY_NAME));
            }
            endpointAddress = namesAndValues.get(ENDPOINT_ADDRESS_NAME);

            maxTelemetryBufferCapacityEnforcer
                    .normalizeStringValue(namesAndValues.get(MAX_TELEMETRY_BUFFER_CAPACITY_NAME));
            sendIntervalInSecondsEnforcer
                    .normalizeStringValue(namesAndValues.get(FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME));
        }

        String maxTransmissionStorageCapacity =
                namesAndValues.get(MAX_TRANSMISSION_STORAGE_CAPACITY_NAME);
        initialize(
                endpointAddress,
                maxTransmissionStorageCapacity,
                developerMode,
                maxTelemetryBufferCapacityEnforcer,
                sendIntervalInSecondsEnforcer,
                throttling,
                maxInstantRetries);
    }

    protected synchronized void initialize(String endpointAddress, String maxTransmissionStorageCapacity,
                                       boolean developerMode, LimitsEnforcer maxTelemetryBufferCapacityEnforcer,
                                       LimitsEnforcer sendIntervalInSeconds, boolean throttling, int maxInstantRetry) {
        makeSureEndpointAddressIsValid(endpointAddress);


        telemetriesTransmitter = getTransmitterFactory().create(endpointAddress, maxTransmissionStorageCapacity, throttling, maxInstantRetry);
        telemetryBuffer = new TelemetryBuffer<>(telemetriesTransmitter, maxTelemetryBufferCapacityEnforcer, sendIntervalInSeconds);

        setDeveloperMode(developerMode);
    }

    protected synchronized TransmitterFactory<T> getTransmitterFactory() {
        if (transmitterFactory == null) {
            transmitterFactory = createTransmitterFactory();
        }
        return transmitterFactory;
    }

    /**
     * Gets value indicating whether this channel is in developer mode.
     */
    @Override
    public boolean isDeveloperMode() {
        return developerMode;
    }

    /**
     * Sets value indicating whether this channel is in developer mode.
     *
     * If true, this also forces maxTelemetriesInBatch to be 1 (affects TelemetryBuffer).
     *
	 * @param developerMode true or false
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
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalLogger.INSTANCE.error("Exception generated while stopping telemetry transmitter");
                InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
    }

    /**
     * Sets the time tow wait before flushing the internal buffer
     *
	 * @param transmitBufferTimeoutInSeconds
	 *            should be between MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS and
	 *            MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS inclusive if the number is
	 *            lower than the minimum then the minimum will be used if the number
	 *            is higher than the maximum then the maximum will be used
     */
    public void setTransmitBufferTimeoutInSeconds(int transmitBufferTimeoutInSeconds) {
        telemetryBuffer.setTransmitBufferTimeoutInSeconds(transmitBufferTimeoutInSeconds);
    }

    /**
     * Sets the buffer size
     *
	 * @param maxTelemetriesInBatch
	 *            should be between MIN_MAX_TELEMETRY_BUFFER_CAPACITY and
	 *            MAX_MAX_TELEMETRY_BUFFER_CAPACITY inclusive if the number is lower
	 *            than the minimum then the minimum will be used if the number is
	 *            higher than the maximum then the maximum will be used
     */
    public void setMaxTelemetriesInBatch(int maxTelemetriesInBatch) {
        telemetryBuffer.setMaxTelemetriesInBatch(maxTelemetriesInBatch);
    }

    /**
     * Flushes the data that the channel might have internally.
     */
    @Override
    public void flush() {
        telemetryBuffer.flush();
    }

    /**
	 * Sets an optional Sampler that can sample out telemetries Currently, we don't
	 * allow to replace a valid telemtry sampler.
     *
	 * @param telemetrySampler
	 *            - The sampler
     */
    @Override
    public void setSampler(TelemetrySampler telemetrySampler) {
        if (this.telemetrySampler == null) {
            this.telemetrySampler = telemetrySampler;
        }
    }

    /**
     * Sends a Telemetry instance through the channel.
     */
    @Override
    public void send(Telemetry telemetry) {
        Preconditions.checkNotNull(telemetry, "Telemetry item must be non null");

        if (isDeveloperMode()) {
            telemetry.getContext().getProperties().put("DeveloperMode", "true");
        }

        if (telemetrySampler != null) {
            if (!telemetrySampler.isSampledIn(telemetry)) {
                return;
            }
        }

        if (!doSend(telemetry)) {
            return;
        }

        if (itemsSent.incrementAndGet() % LOG_TELEMETRY_ITEMS_MODULUS == 0) {
            InternalLogger.INSTANCE.info("items sent till now %d", itemsSent.get());
        }

        if (isDeveloperMode()) {
            writeTelemetryToDebugOutput(telemetry);
        }
    }

    /**
     *
     * @param telemetry
     * @return true, if the send was successful, false if there was an error
     */
    protected abstract boolean doSend(Telemetry telemetry);

    private void writeTelemetryToDebugOutput(Telemetry telemetry) {
        InternalLogger.INSTANCE.trace("%s sending telemetry: %s", this.getClass().getSimpleName(), telemetry.toString());
    }

    protected abstract TransmitterFactory<T> createTransmitterFactory();

    protected LimitsEnforcer createDefaultMaxTelemetryBufferCapacityEnforcer(Integer currentValue) {
		LimitsEnforcer maxItemsInBatchEnforcer = LimitsEnforcer.createWithClosestLimitOnError(
                MAX_TELEMETRY_BUFFER_CAPACITY_NAME, MIN_MAX_TELEMETRY_BUFFER_CAPACITY,
				MAX_MAX_TELEMETRY_BUFFER_CAPACITY, DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY,
                        currentValue == null ? DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY : currentValue);

        return maxItemsInBatchEnforcer;
    }

    protected LimitsEnforcer createDefaultSendIntervalInSecondsEnforcer(Integer currentValue) {
		LimitsEnforcer sendIntervalInSecondsEnforcer = LimitsEnforcer.createWithClosestLimitOnError(
				FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME, MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS,
				MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS, DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS,
                        currentValue == null ? DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS : currentValue);

        return sendIntervalInSecondsEnforcer;
    }

    /**
	 * The method will throw IllegalArgumentException if the endpointAddress is not
	 * a valid URI. Please note that a null or empty string is valid as far as the
	 * class is concerned and thus considered valid
     *
     * @param endpointAddress
     */
    protected void makeSureEndpointAddressIsValid(String endpointAddress) {
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
}
