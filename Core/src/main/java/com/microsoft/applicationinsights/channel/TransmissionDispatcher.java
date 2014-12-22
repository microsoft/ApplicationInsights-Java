package com.microsoft.applicationinsights.channel;

import java.util.concurrent.TimeUnit;

/**
 * A dispatcher should know how and to whom to dispatch a {@link com.microsoft.applicationinsights.channel.Transmission}
 *
 * Created by gupele on 12/18/2014.
 */
public interface TransmissionDispatcher {
    void dispatch(Transmission transmission);

    void stop(long timeout, TimeUnit timeUnit);
}

