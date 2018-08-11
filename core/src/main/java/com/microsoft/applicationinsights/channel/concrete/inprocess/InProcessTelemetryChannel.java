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

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.channel.concrete.ATelemetryChannel;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LimitsEnforcer;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.Sanitizer;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * An implementation of {@link com.microsoft.applicationinsights.channel.TelemetryChannel}
 *
 * <p>The channel holds two main entities:
 *
 * <p>A buffer for incoming {@link com.microsoft.applicationinsights.telemetry.Telemetry} instances
 * A transmitter
 *
 * <p>The buffer is stores incoming telemetry instances. Every new buffer starts a timer. When the
 * timer expires, or when the buffer is 'full' (whichever happens first), the transmitter will pick
 * up that buffer and will handle its sending to the server. For example, a transmitter will be
 * responsible for compressing, sending and activate a policy in case of failures.
 *
 * <p>The model here is:
 *
 * <p>Use application threads to populate the buffer Use channel's threads to send buffers to the
 * server
 *
 * <p>Created by gupele on 12/17/2014.
 */
public final class InProcessTelemetryChannel extends ATelemetryChannel<String> {

    public InProcessTelemetryChannel() {
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
		initialize(null, null, developerMode, createDefaultMaxTelemetryBufferCapacityEnforcer(null),
				createDefaultSendIntervalInSecondsEnforcer(null), true);
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
  public InProcessTelemetryChannel(
      String endpointAddress,
      boolean developerMode,
      int maxTelemetryBufferCapacity,
      int sendIntervalInMillis) {

    // Builder pattern implementation
    this(
        endpointAddress,
        null,
        developerMode,
        maxTelemetryBufferCapacity,
        sendIntervalInMillis,
        true,
        DEFAULT_MAX_INSTANT_RETRY);
  }

  public InProcessTelemetryChannel(
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
	 * @param namesAndValues
	 *            - The data passed as name and value pairs
     */
    public InProcessTelemetryChannel(Map<String, String> namesAndValues) {
        boolean developerMode = false;
        String endpointAddress = null;
		int maxInstantRetries = DEFAULT_MAX_INSTANT_RETRY;

        LimitsEnforcer maxTelemetryBufferCapacityEnforcer = createDefaultMaxTelemetryBufferCapacityEnforcer(null);

        LimitsEnforcer sendIntervalInSecondsEnforcer = createDefaultSendIntervalInSecondsEnforcer(null);

        boolean throttling = true;
        if (namesAndValues != null) {
            throttling = Boolean.valueOf(namesAndValues.get("Throttling"));
            developerMode = Boolean.valueOf(namesAndValues.get(DEVELOPER_MODE_NAME));
			try {
				String instantRetryValue = namesAndValues.get(INSTANT_RETRY_NAME);
				if (instantRetryValue != null){
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
					.normalizeStringValue(namesAndValues.get(MAX_MAX_TELEMETRY_BUFFER_CAPACITY_NAME));
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

    @Override
    protected boolean doSend(Telemetry telemetry) {
        StringWriter writer = new StringWriter();
        JsonTelemetryDataSerializer jsonWriter = null;
        try {
            jsonWriter = new JsonTelemetryDataSerializer(writer);
            telemetry.serialize(jsonWriter);
            jsonWriter.close();
            String asJson = writer.toString();
            telemetryBuffer.add(asJson);
            telemetry.reset();

        } catch (IOException e) {
            InternalLogger.INSTANCE.error("Failed to serialize Telemetry");
            InternalLogger.INSTANCE.trace("Stack trace is %s", ExceptionUtils.getStackTrace(e));
            return false;
        }
        return true;
    }

    private synchronized void initialize(String endpointAddress, String maxTransmissionStorageCapacity,
			boolean developerMode, LimitsEnforcer maxTelemetryBufferCapacityEnforcer,
			LimitsEnforcer sendIntervalInSeconds, boolean throttling) {
		initialize(endpointAddress, maxTransmissionStorageCapacity, developerMode, maxTelemetryBufferCapacityEnforcer,
				sendIntervalInSeconds, throttling, DEFAULT_MAX_INSTANT_RETRY);
	}

	private synchronized void initialize(String endpointAddress, String maxTransmissionStorageCapacity,
			boolean developerMode, LimitsEnforcer maxTelemetryBufferCapacityEnforcer,
			LimitsEnforcer sendIntervalInSeconds, boolean throttling, int maxInstantRetry) {
        makeSureEndpointAddressIsValid(endpointAddress);

        if (s_transmitterFactory == null) {
            s_transmitterFactory = new InProcessTelemetryTransmitterFactory();
        }

		telemetriesTransmitter = s_transmitterFactory.create(endpointAddress, maxTransmissionStorageCapacity,
				throttling, maxInstantRetry);
		telemetryBuffer = new TelemetryBuffer(telemetriesTransmitter, maxTelemetryBufferCapacityEnforcer,
				sendIntervalInSeconds);

        setDeveloperMode(developerMode);
    }

    /**
	 * The method will throw IllegalArgumentException if the endpointAddress is not
	 * a valid uri Please note that a null or empty string is valid as far as the
	 * class is concerned and thus considered valid
     *
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
		LimitsEnforcer maxItemsInBatchEnforcer = LimitsEnforcer.createWithClosestLimitOnError(
				MAX_MAX_TELEMETRY_BUFFER_CAPACITY_NAME, MIN_MAX_TELEMETRY_BUFFER_CAPACITY,
				MAX_MAX_TELEMETRY_BUFFER_CAPACITY, DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY,
                        currentValue == null ? DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY : currentValue);

        return maxItemsInBatchEnforcer;
    }

    private LimitsEnforcer createDefaultSendIntervalInSecondsEnforcer(Integer currentValue) {
		LimitsEnforcer sendIntervalInSecondsEnforcer = LimitsEnforcer.createWithClosestLimitOnError(
				FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME, MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS,
				MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS, DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS,
                        currentValue == null ? DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS : currentValue);

        return sendIntervalInSecondsEnforcer;
    }

}
