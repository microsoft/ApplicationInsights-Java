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

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LimitsEnforcer;
import com.microsoft.applicationinsights.internal.util.Sanitizer;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

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
    private final static int DEFAULT_NUMBER_OF_TELEMETRIES_IN_BATCH = 500;
    private final static int MIN_NUMBER_OF_TELEMETRIES_IN_BATCH = 1;
    private final static int MAX_NUMBER_OF_TELEMETRIES_IN_BATCH = 1000;
    private final static String MAX_NUMBER_OF_TELEMETRIES_IN_BATCH_NAME = "MaxTelemetryItemsInQueue";

    private final static int DEFAULT_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS = 10;
    private final static int MIN_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS = 1;
    private final static int MAX_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS = 300;
    private final static String TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS_NAME = "SendIntervalInSeconds";

    private final static String DEVELOPER_MODE_NAME = "DeveloperMode";
    private final static String ENDPOINT_ADDRESS_NAME = "EndpointAddress";

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
                   developerMode,
                   createDefaultMaxItemsInBatchEnforcer(null),
                   createDefaultSendIntervalInSecondsEnforcer(null));
    }

    /**
     * Ctor
     * @param endpointAddress Must be empty string or a valid uri, else an exception will be thrown
     * @param developerMode True will behave in a 'non-production' mode to ease the debugging
     * @param maxQueueItemCount Max number of Telemetries we keep in the buffer, when reached we will send the buffer
     *                          Note, value should be between TRANSMIT_BUFFER_MIN_TIMEOUT_IN_MILLIS and TRANSMIT_BUFFER_MAX_TIMEOUT_IN_MILLIS inclusive
     * @param sendIntervalInMillis The maximum number of milliseconds to wait before we send the buffer
     *                          Note, value should be between MIN_NUMBER_OF_TELEMETRIES_IN_BATCH and MAX_NUMBER_OF_TELEMETRIES_IN_BATCH inclusive
     */
    public InProcessTelemetryChannel(String endpointAddress, boolean developerMode, int maxQueueItemCount, int sendIntervalInMillis) {
        initialize(endpointAddress,
                   developerMode,
                   createDefaultMaxItemsInBatchEnforcer(maxQueueItemCount),
                   createDefaultSendIntervalInSecondsEnforcer(sendIntervalInMillis));
    }

    /**
     * This Ctor will query the 'namesAndValues' map for data to initialize itself
     * It will ignore data that is not of its interest, this Ctor is useful for building an instance from configuration
     * @param nameAndValues - The data passed as name and value pairs
     */
    public InProcessTelemetryChannel(Map<String, String> nameAndValues) {
        boolean developerMode = false;
        String endpointAddress = null;

        LimitsEnforcer maxItemsInBatchEnforcer = createDefaultMaxItemsInBatchEnforcer(null);

        LimitsEnforcer sendIntervalInSecondsEnforcer = createDefaultSendIntervalInSecondsEnforcer(null);

        if (nameAndValues != null) {
            developerMode = Boolean.valueOf(nameAndValues.get(DEVELOPER_MODE_NAME));
            endpointAddress = nameAndValues.get(ENDPOINT_ADDRESS_NAME);

            maxItemsInBatchEnforcer.normalizeStringValue(nameAndValues.get(MAX_NUMBER_OF_TELEMETRIES_IN_BATCH_NAME));
            sendIntervalInSecondsEnforcer.normalizeStringValue(nameAndValues.get(TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS_NAME));
        }

        initialize(endpointAddress, developerMode, maxItemsInBatchEnforcer, sendIntervalInSecondsEnforcer);
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
            int maxTelemetriesInBatch = this.developerMode ? 1 : DEFAULT_NUMBER_OF_TELEMETRIES_IN_BATCH;

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
     * @param maxTelemetriesInBatch should be between MIN_NUMBER_OF_TELEMETRIES_IN_BATCH
     *                              and MAX_NUMBER_OF_TELEMETRIES_IN_BATCH inclusive
     *                              if the number is lower than the minimum then the minimum will be used
     *                              if the number is higher than the maximum then the maximum will be used
     */
    public void setMaxTelemetriesInBatch(int maxTelemetriesInBatch) {
        telemetryBuffer.setMaxTelemetriesInBatch(maxTelemetriesInBatch);
    }

    /**
     * Sets the time tow wait before flushing the internal buffer
     * @param transmitBufferTimeoutInSeconds should be between MIN_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS
     *                                       and MAX_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS inclusive
     *                                       if the number is lower than the minimum then the minimum will be used
     *                                       if the number is higher than the maximum then the maximum will be used
     */
    public void setTransmitBufferTimeoutInSeconds(int transmitBufferTimeoutInSeconds) {
        telemetryBuffer.setTransmitBufferTimeoutInSeconds(transmitBufferTimeoutInSeconds);
    }

    private void writeTelemetryToDebugOutput(Telemetry telemetry) {
        InternalLogger.INSTANCE.trace("InProcessTelemetryChannel sending telemetry");
    }

    private synchronized void initialize(String endpointAddress, boolean developerMode, LimitsEnforcer maxItemsInBatch, LimitsEnforcer sendIntervalInSeconds) {
        makeSureEndpointAddressIsValid(endpointAddress);

        if (s_transmitterFactory == null) {
            s_transmitterFactory = new InProcessTelemetryChannelFactory();
        }

        telemetriesTransmitter = s_transmitterFactory.create(endpointAddress);
        telemetryBuffer = new TelemetryBuffer(telemetriesTransmitter, maxItemsInBatch, sendIntervalInSeconds);

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

    /**
     * The method will translate the numbers and will make sure it is within the limits
     * If the value is not an int the default value will be returned
     * Else, will return the outcome of 'makeSureNumberIsWithinLimits'
     * @param propertyName The value to translate
     * @param minimum The minimum value allowed, the value must be >= to that value
     * @param maximum The maximum value allowed, the value must be <= to that value
     * @param defaultValue Return this value in case the valueString is not an int
     * @param propertyName The name of the property to display if needed in log messages
     * @return The value within limits as stated in the param declarations above
     */
    private int translateNumber(Map<String, String> values, int minimum, int maximum, int defaultValue, String propertyName) {
        String valueString = null;
        try {
            valueString = values.get(propertyName);
            if (Strings.isNullOrEmpty(valueString)) {
                return defaultValue;
            }

            int value = Integer.parseInt(valueString);
            return makeSureNumberIsWithinLimits(value, minimum, maximum, propertyName);
        } catch (NumberFormatException e) {
            InternalLogger.INSTANCE.trace("'%s': bad format for value '%s', therefore using '%d'", propertyName, valueString, defaultValue);

            return defaultValue;
        }
    }

    /**
     * Will return the value if (value >= minimum and value <= maximum) otherwise the limit that is closer
     * @param value The value to check
     * @param minimum The value should be >= from that value
     * @param maximum The Value should be <= from that value
     * @param propertyName The property name to display in log messages
     * @return Will return the value if (value >= minimum and value <= maximum) otherwise the limit that is closer
     */
    private int makeSureNumberIsWithinLimits(int value, int minimum, int maximum, String propertyName) {
        if (value < minimum) {
            InternalLogger.INSTANCE.trace("'%s': value '%d' is lower than the limit '%d', therefore the limit will be used", propertyName, value, minimum);

            return minimum;
        } else if (value > maximum) {
            InternalLogger.INSTANCE.trace("'%s': value '%d' is higher than the limit '%d', therefore the limit will be used", propertyName, value, maximum);

            return maximum;
        }

        return value;
    }

    private LimitsEnforcer createDefaultMaxItemsInBatchEnforcer(Integer currentValue) {
        LimitsEnforcer maxItemsInBatchEnforcer =
                LimitsEnforcer.createWithClosestLimitOnError(MAX_NUMBER_OF_TELEMETRIES_IN_BATCH_NAME,
                                                             MIN_NUMBER_OF_TELEMETRIES_IN_BATCH,
                                                             MAX_NUMBER_OF_TELEMETRIES_IN_BATCH,
                                                             DEFAULT_NUMBER_OF_TELEMETRIES_IN_BATCH,
                                                             currentValue == null ? DEFAULT_NUMBER_OF_TELEMETRIES_IN_BATCH : currentValue);

        return maxItemsInBatchEnforcer;
    }

    private LimitsEnforcer createDefaultSendIntervalInSecondsEnforcer(Integer currentValue) {
        LimitsEnforcer sendIntervalInSecondsEnforcer =
                LimitsEnforcer.createWithClosestLimitOnError(TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS_NAME,
                                                             MIN_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS,
                                                             MAX_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS,
                                                             DEFAULT_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS,
                                                             currentValue == null ? DEFAULT_TRANSMIT_BUFFER_TIMEOUT_IN_SECONDS : currentValue);

        return sendIntervalInSecondsEnforcer;
    }
}
