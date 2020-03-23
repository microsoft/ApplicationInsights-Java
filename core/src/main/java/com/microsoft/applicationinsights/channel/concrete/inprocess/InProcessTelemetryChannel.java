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

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.concrete.TelemetryChannelBase;
import com.microsoft.applicationinsights.internal.channel.ConfiguredTransmitterFactory;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.Map;

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
public final class InProcessTelemetryChannel extends TelemetryChannelBase<Telemetry> {

    public InProcessTelemetryChannel(TelemetryConfiguration configuration) {
        super(configuration);
    }

    public InProcessTelemetryChannel(TelemetryConfiguration configuration, String maxTransmissionStorageCapacity, boolean developerMode, int maxTelemetryBufferCapacity, int sendIntervalInMillis, boolean throttling, int maxInstantRetries) {
        super(configuration, maxTransmissionStorageCapacity, developerMode, maxTelemetryBufferCapacity, sendIntervalInMillis, throttling, maxInstantRetries);
    }

    public InProcessTelemetryChannel(TelemetryConfiguration configuration, Map<String, String> channelConfig) {
        super(configuration, channelConfig);
    }

    @Deprecated
    public InProcessTelemetryChannel() {
        super();
    }

    @Deprecated
    public InProcessTelemetryChannel(String endpointAddress, boolean developerMode, int maxTelemetryBufferCapacity, int sendIntervalInMillis) {
        super(endpointAddress, developerMode, maxTelemetryBufferCapacity, sendIntervalInMillis);
    }

    @Deprecated
    public InProcessTelemetryChannel(String endpointAddress, String maxTransmissionStorageCapacity, boolean developerMode, int maxTelemetryBufferCapacity, int sendIntervalInMillis, boolean throttling, int maxInstantRetries) {
        super(endpointAddress, maxTransmissionStorageCapacity, developerMode, maxTelemetryBufferCapacity, sendIntervalInMillis, throttling, maxInstantRetries);
    }

    /**
     * @deprecated Use {@link #InProcessTelemetryChannel(TelemetryConfiguration, Map)}
     */
    @Deprecated
    public InProcessTelemetryChannel(Map<String, String> namesAndValues) {
        super(namesAndValues);
    }

    @Override
    protected boolean doSend(Telemetry telemetry) {
        // this is temporary until we are convinced that telemetry are never re-used by codeless agent
        if (telemetry.previouslyUsed()) {
            throw new IllegalStateException("Telemetry was previously used: " + telemetry);
        }
        telemetryBuffer.add(telemetry);
        return true;
    }

    @Override
    protected ConfiguredTransmitterFactory<Telemetry> createTransmitterFactory() {
        return new InProcessTelemetryTransmitterFactory();
    }

}
