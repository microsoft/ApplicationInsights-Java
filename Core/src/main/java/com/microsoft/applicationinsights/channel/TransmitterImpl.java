package com.microsoft.applicationinsights.channel;

import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.util.ThreadPoolUtils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * The default implementation of the {@link TelemetriesTransmitter}
 *
 * The class is responsible holds the classes that do the actual sending to the server
 * Telemetry instances buffered in a collection are sent through this class.
 *
 * The class makes sure that the container of telemetries is sent using internal threads
 * and not the 'application' threads
 *
 * Created by gupele on 12/18/2014.
 */
public class TransmitterImpl implements TelemetriesTransmitter {
    private static abstract class SendHandler {
        protected final TransmissionDispatcher transmissionDispatcher;

        protected final TelemetrySerializer serializer;

        protected SendHandler(TransmissionDispatcher transmissionDispatcher, TelemetrySerializer serializer) {
            Preconditions.checkNotNull(transmissionDispatcher, "transmissionDispatcher should be a non-null value");
            Preconditions.checkNotNull(serializer, "serializer should be a non-null value");

            this.transmissionDispatcher = transmissionDispatcher;
            this.serializer = serializer;
        }

        protected void dispatch(Collection<Telemetry> telemetries) {
            if (telemetries.isEmpty()) {
                return;
            }

            Optional<Transmission> transmission = serializer.serialize(telemetries);
            if (!transmission.isPresent()) {
                return;
            }

            transmissionDispatcher.dispatch(transmission.get());
        }
    }

    private static final class ScheduledSendHandler extends SendHandler implements Runnable {
        private final TelemetriesFetcher telemetriesFetcher;

        public ScheduledSendHandler(TransmissionDispatcher transmissionDispatcher, TelemetriesFetcher telemetriesFetcher, TelemetrySerializer serializer) {
            super(transmissionDispatcher,  serializer);

            Preconditions.checkNotNull(telemetriesFetcher, "telemetriesFetcher should be a non-null value");

            this.telemetriesFetcher = telemetriesFetcher;
        }

        @Override
        public void run() {
            Collection<Telemetry> telemetriesToSend = telemetriesFetcher.fetch();
            dispatch(telemetriesToSend);
        }
    }

    private static final class SendNowHandler extends SendHandler implements Runnable {
        private final Collection<Telemetry> telemetries;

        public SendNowHandler(TransmissionDispatcher transmissionDispatcher, TelemetrySerializer serializer, Collection<Telemetry> telemetries) {
            super(transmissionDispatcher,  serializer);

            Preconditions.checkNotNull(telemetries, "telemetries should be non-null value");

            this.telemetries = telemetries;
        }

        @Override
        public void run() {
            dispatch(telemetries);
        }
    }

    private final TransmissionDispatcher transmissionDispatcher;

    private final TelemetrySerializer serializer;

    private final ScheduledThreadPoolExecutor threadPool;

    private final TransmissionsLoader transmissionsLoader;

    public TransmitterImpl(TransmissionDispatcher transmissionDispatcher, TelemetrySerializer serializer, TransmissionsLoader transmissionsLoader) {
        this.transmissionDispatcher = transmissionDispatcher;
        this.serializer = serializer;
        threadPool = new ScheduledThreadPoolExecutor(2);
        this.transmissionsLoader = transmissionsLoader;
        this.transmissionsLoader.load(false);
    }

    @Override
    public void scheduleSend(TelemetriesFetcher telemetriesFetcher, long value, TimeUnit timeUnit) {
        Preconditions.checkNotNull(telemetriesFetcher, "telemetriesFetcher should be non-null value");

        threadPool.schedule(new ScheduledSendHandler(transmissionDispatcher, telemetriesFetcher, serializer), value, timeUnit);
    }

    @Override
    public void sendNow(Collection<Telemetry> telemetries) {
        Preconditions.checkNotNull(telemetries, "telemetries should be non-null value");

        threadPool.submit(new SendNowHandler(transmissionDispatcher, serializer, telemetries));
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        transmissionsLoader.stop(timeout, timeUnit);
        ThreadPoolUtils.stop(threadPool, timeout, timeUnit);
        transmissionDispatcher.stop(timeout, timeUnit);
    }
}
