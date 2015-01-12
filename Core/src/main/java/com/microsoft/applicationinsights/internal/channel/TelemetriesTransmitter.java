package com.microsoft.applicationinsights.internal.channel;

import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * The class is responsible for getting containers of {@link com.microsoft.applicationinsights.telemetry.Telemetry},
 * transform them into {@link com.microsoft.applicationinsights.internal.channel.common.Transmission} and
 * then initiate the sending process.
 *
 * Containers of Telemetry instances are populated by application threads. This class use
 * the 'channel's' threads for the rest of the process. In other words, the de-coupling of
 * user and channel threads happens here.
 *
 * The class lets its users to schedule a 'send', where a channel thread will be sent to 'pick up'
 * the container of Telemetries.
 * Or, it also lets the caller to initiate a 'send now' call where the caller passes the container
 * and this class will continue, again, using a channel thread while releasing the calling thread.
 *
 * Created by gupele on 12/17/2014.
 */
public interface TelemetriesTransmitter {
    public interface TelemetriesFetcher {
        Collection<Telemetry> fetch();
    }

    void scheduleSend(TelemetriesFetcher telemetriesFetcher, long value, TimeUnit timeUnit);

    void sendNow(Collection<Telemetry> telemetries);

    void stop(long timeout, TimeUnit timeUnit);
}
