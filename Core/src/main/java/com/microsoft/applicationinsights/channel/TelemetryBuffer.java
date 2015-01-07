package com.microsoft.applicationinsights.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

/**
 * The class is responsible for getting instances of {@link com.microsoft.applicationinsights.channel.Telemetry}
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
final class TelemetryBuffer {
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
        public Collection<Telemetry> fetch() {
            synchronized (lock) {
                if (expectedGeneration != generation) {
                    return Collections.emptyList();
                }

                ++generation;
                List<Telemetry> readyToBeSent = telemetries;
                telemetries = new ArrayList<Telemetry>();

                return readyToBeSent;
            }
        }
    }

    private final static int DEFAULT_NUMBER_OF_TELEMETRIES_PER_CONTAINER = 128;

    private final static int TRANSMIT_BUFFER_DEFAULT_TIMEOUT_IN_SECONDS = 10;

    /// The sender we use to send Telemetry containers
    private final TelemetriesTransmitter sender;

    /// The maximum amount of Telemetries in a batch. If the buffer is
    /// full before the timeout expired, we will need to send it anyway and not wait for the timeout to expire
    private final int maxTelemetriesInBatch;

    private final int transmitBufferTimeoutInSeconds;

    /// The Telemetry instances are kept here
    private List<Telemetry> telemetries;

    /// A way to help incoming threads make sure they are picking up the right Telemetry container
    private long generation = 0;

    /// A synchronization object to avoid race conditions with the container and generation
    private final Object lock = new Object();

    private boolean developerMode;

    /**
     * The constructor needs to get the 'sender' we work with
     * @param sender
     */
    public TelemetryBuffer(TelemetriesTransmitter sender, boolean developerMode) {
        this(sender, DEFAULT_NUMBER_OF_TELEMETRIES_PER_CONTAINER, TRANSMIT_BUFFER_DEFAULT_TIMEOUT_IN_SECONDS, developerMode);
    }

    /**
     * The constructor needs to get the 'sender' we work with
     * @param sender
     */
    public TelemetryBuffer(TelemetriesTransmitter sender, int maxTelemetriesInBatch, int transmitBufferTimeoutInSeconds, boolean developerMode) {
        Preconditions.checkNotNull(sender, "sender must be non-null value");
        Preconditions.checkArgument(maxTelemetriesInBatch > 0, "maxTelemetriesInBatch must be a positive number");
        Preconditions.checkArgument(transmitBufferTimeoutInSeconds > 0, "transmitBufferTimeoutInSeconds must be a positive number");

        this.developerMode = developerMode;
        if (developerMode) {
            this.maxTelemetriesInBatch = 1;
        } else {
            this.maxTelemetriesInBatch = maxTelemetriesInBatch;
        }

        this.sender = sender;
        telemetries = new ArrayList<Telemetry>(this.maxTelemetriesInBatch);
        this.transmitBufferTimeoutInSeconds = transmitBufferTimeoutInSeconds;
    }

    /**
     * The method will add the incoming {@see Telemetry} to its internal container of Telemetries
     *
     * If that is the first instance in the container, we schedule a 'pick-up' in a configurable amount of time
     * If by adding that item we exceeded the maximum number of instances, we trigger a send request now.
     *
     * Note that a lock is used to make sure we avoid race conditions and to make sure that we cleanly
     * move from a ready to send buffer to a new one
     * @param telemetry
     */
    public void add(Telemetry telemetry) {
        Preconditions.checkNotNull(telemetry, "Telemetry must be non null value");

        synchronized (lock) {
            telemetries.add(telemetry);

            int currentSize = telemetries.size();

            if (currentSize >= maxTelemetriesInBatch) {
                sender.sendNow(prepareTelemetriesForSend());
            } else if (currentSize == 1) {
                sender.scheduleSend(new TelemetryBufferTelemetriesFetcher(generation), transmitBufferTimeoutInSeconds, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * The method assumes that the lock is held before calling it.
     *
     * Please make sure this behavior is kept to avoid unknown scenarios
     *
     * @return The list of {@see Telemetry} instances that are ready to be sent
     */
    private List<Telemetry> prepareTelemetriesForSend() {
        ++generation;

        final List<Telemetry> readyToBeSent = telemetries;

        telemetries = new ArrayList<Telemetry>(maxTelemetriesInBatch);

        return readyToBeSent;
    }
}
