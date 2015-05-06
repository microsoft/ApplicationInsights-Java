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

package com.microsoft.applicationinsights.internal.channel.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * The class is responsible for getting instances of {@link com.microsoft.applicationinsights.telemetry.Telemetry}
 *
 * It is responsible for managing the buffers. Once a new buffer is created it will schedule a call to
 * pick up the buffer.
 *
 * If the buffer is full before the timeout expired, it will initiate a 'send now' activity to send the buffer asap.
 *
 * The class is responsible for handing the corner cases that might rise
 *
 * Created by gupele on 12/17/2014.
 */
public final class TelemetryBuffer {
    /**
     * An inner helper class that will let the Sender class to fetch the relevant Telemetries.
     *
     * The class assumes it needs to work with Telemetries of 'expectedGeneration'. If that generation
     * is no valid anymore, nothing will be sent.
     * Else, a new buffer is created, the generation is incremented and the 'ready' buffer is sent
     */
    private final class TelemetryBufferTelemetriesFetcher implements TelemetriesTransmitter.TelemetriesFetcher {

        private final long expectedGeneration;

        private TelemetryBufferTelemetriesFetcher(long expectedGeneration) {
            this.expectedGeneration = expectedGeneration;
        }

        @Override
        public Collection<String> fetch() {
            synchronized (lock) {
                if (expectedGeneration != generation) {
                    return Collections.emptyList();
                }

                ++generation;
                List<String> readyToBeSent = telemetries;
                telemetries = new ArrayList<String>();

                return readyToBeSent;
            }
        }
    }

    /// The sender we use to send Telemetry containers
    private final TelemetriesTransmitter sender;

    /// The maximum amount of Telemetries in a batch. If the buffer is
    /// full before the timeout expired, we will need to send it anyway and not wait for the timeout to expire
    private int maxTelemetriesInBatch;

    private int transmitBufferTimeoutInSeconds;

    /// The Telemetry instances are kept here
    private List<String> telemetries;

    /// A way to help incoming threads make sure they are picking up the right Telemetry container
    private long generation = 0;

    /// A synchronization object to avoid race conditions with the container and generation
    private final Object lock = new Object();

    /**
     * The constructor needs to get the 'sender' we work with
     * @param sender The sender object for transmitting the telemetries
     * @param maxTelemetriesInBatch The maximum number of telemetries in a batch
     * @param transmitBufferTimeoutInSeconds The transmit buffer timeout in seconds
     */
    public TelemetryBuffer(TelemetriesTransmitter sender, int maxTelemetriesInBatch, int transmitBufferTimeoutInSeconds) {
        Preconditions.checkNotNull(sender, "sender must be non-null value");
        Preconditions.checkArgument(maxTelemetriesInBatch > 0, "maxTelemetriesInBatch must be a positive number");
        Preconditions.checkArgument(transmitBufferTimeoutInSeconds > 0, "transmitBufferTimeoutInSeconds must be a positive number");

        setMaxTelemetriesInBatch(maxTelemetriesInBatch);

        this.sender = sender;
        telemetries = new ArrayList<String>(this.maxTelemetriesInBatch);
        this.transmitBufferTimeoutInSeconds = transmitBufferTimeoutInSeconds;
    }

    /**
     * Sets the maximum number of telemetries in a batch
     * @param maxTelemetriesInBatch The max amount of Telemetries that are allowed in a batch.
     */
    public void setMaxTelemetriesInBatch(int maxTelemetriesInBatch) {
        synchronized (lock) {
            if (telemetries != null && maxTelemetriesInBatch < telemetries.size()) {
                // Request for smaller buffers, we flush if our buffer contains more elements
                flush();
            }
            this.maxTelemetriesInBatch = maxTelemetriesInBatch;
        }
    }

    /**
     * Gets the maximum number of telemetries in a batch
     * @return The maximum number of telemetries in a batch
     */
    public int getMaxTelemetriesInBatch() {
        return this.maxTelemetriesInBatch;
    }

    /**
     * Sets the transmit buffer timeout in seconds
     * @param transmitBufferTimeoutInSeconds The amount of time to wait before sending the buffer.
     */
    public void setTransmitBufferTimeoutInSeconds(int transmitBufferTimeoutInSeconds) {
        synchronized (lock) {
            // Request for quicker flushes, we flush if the previous timeout is bigger
            if (transmitBufferTimeoutInSeconds < this.transmitBufferTimeoutInSeconds) {
                flush();
            }
            this.transmitBufferTimeoutInSeconds = transmitBufferTimeoutInSeconds;
        }
    }

    /**
     * Gets the transmit buffer timeout in seconds
     * @return The transmit buffer timeout in seconds
     */
    public int getTransmitBufferTimeoutInSeconds() {
        return this.transmitBufferTimeoutInSeconds;
    }

    /**
     * The method will add the incoming {@link Telemetry} to its internal container of Telemetries
     *
     * If that is the first instance in the container, we schedule a 'pick-up' in a configurable amount of time
     * If by adding that item we exceeded the maximum number of instances, we trigger a send request now.
     *
     * Note that a lock is used to make sure we avoid race conditions and to make sure that we cleanly
     * move from a ready to send buffer to a new one
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to add to the buffer.
     */
    public void add(String telemetry) {
        Preconditions.checkNotNull(telemetry, "Telemetry must be non null value");

        synchronized (lock) {
            telemetries.add(telemetry);

            int currentSize = telemetries.size();

            if (currentSize >= maxTelemetriesInBatch) {
                if (!sender.sendNow(prepareTelemetriesForSend())) {
                    // 'prepareTelemetriesForSend' already created a new container
                    // so basically we have nothing to do, the old container is lost
                    InternalLogger.INSTANCE.error("Failed to send buffer data to network");
                }
            } else if (currentSize == 1) {
                if (!sender.scheduleSend(new TelemetryBufferTelemetriesFetcher(generation), transmitBufferTimeoutInSeconds, TimeUnit.SECONDS)) {
                    // We cannot schedule send so we give up the Telemetry
                    // The reason for this is that in case the maximum buffer size is greater than 2
                    // than in case a new Telemetry arrives it won't trigger the schedule and might be lost too
                    InternalLogger.INSTANCE.error("Failed to schedule send of the buffer to network");
                    telemetries.clear();
                }
            }
        }
    }

    /**
     * The method will flush the telemetries currently in the buffer to the {@link com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter}
     */
    public void flush() {
        synchronized (lock) {
            if (telemetries.size() != 0) {
                if (!sender.sendNow(prepareTelemetriesForSend())) {
                    InternalLogger.INSTANCE.error("Failed to flush buffer data to network");
                }
            }
        }
    }

    /**
     * The method assumes that the lock is held before calling it.
     *
     * Please make sure this behavior is kept to avoid unknown scenarios
     *
     * @return The list of {@link Telemetry} instances that are ready to be sent
     */
    private List<String> prepareTelemetriesForSend() {
        ++generation;

        final List<String> readyToBeSent = telemetries;

        telemetries = new ArrayList<String>(maxTelemetriesInBatch);

        return readyToBeSent;
    }
}
