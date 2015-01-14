package com.microsoft.applicationinsights.internal.channel.common;

import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TelemetrySerializer;
import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionsLoader;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.telemetry.Telemetry;

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
public final class TransmitterImpl implements TelemetriesTransmitter {
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

    private final static int MAX_PENDING_SCHEDULE_REQUESTS = 2048;

    private final TransmissionDispatcher transmissionDispatcher;

    private final TelemetrySerializer serializer;

    private final ScheduledThreadPoolExecutor threadPool;

    private final TransmissionsLoader transmissionsLoader;

    private final Semaphore semaphore;

    public TransmitterImpl(TransmissionDispatcher transmissionDispatcher, TelemetrySerializer serializer, TransmissionsLoader transmissionsLoader) {
        this.transmissionDispatcher = transmissionDispatcher;
        this.serializer = serializer;

        semaphore = new Semaphore(MAX_PENDING_SCHEDULE_REQUESTS);

        threadPool = new ScheduledThreadPoolExecutor(2);
        threadPool.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });

        this.transmissionsLoader = transmissionsLoader;
        this.transmissionsLoader.load(false);
    }

    @Override
    public boolean scheduleSend(TelemetriesFetcher telemetriesFetcher, long value, TimeUnit timeUnit) {
        Preconditions.checkNotNull(telemetriesFetcher, "telemetriesFetcher should be non-null value");

        if (!semaphore.tryAcquire()) {
            return false;
        }

        try {
            final Runnable command = new ScheduledSendHandler(transmissionDispatcher, telemetriesFetcher, serializer);
            threadPool.schedule(new Runnable() {
                public void run() {
                    try {
                        semaphore.release();
                        command.run();
                    } catch (Exception e) {
                    } catch (Throwable t) {
                    } finally {
                    }
                }
            }, value, timeUnit);

            return true;
        } catch (Exception e) {
            semaphore.release();
        } catch (Throwable t) {
            semaphore.release();
        }

        return true;
    }

    @Override
    public boolean sendNow(Collection<Telemetry> telemetries) {
        Preconditions.checkNotNull(telemetries, "telemetries should be non-null value");

        if (!semaphore.tryAcquire()) {
            return false;
        }

        final Runnable command = new SendNowHandler(transmissionDispatcher, serializer, telemetries);
        try {
            threadPool.execute(new Runnable() {
                public void run() {
                    try {
                        semaphore.release();
                        command.run();
                    } catch (Exception e) {
                    } catch (Throwable t) {
                    } finally {
                    }
                }
            });

            return true;
        } catch (Exception e) {
            semaphore.release();
        } catch (Throwable t) {
            semaphore.release();
        }

        return false;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        transmissionsLoader.stop(timeout, timeUnit);
        ThreadPoolUtils.stop(threadPool, timeout, timeUnit);
        transmissionDispatcher.stop(timeout, timeUnit);
    }
}
