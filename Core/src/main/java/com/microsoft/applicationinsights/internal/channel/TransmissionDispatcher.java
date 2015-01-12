package com.microsoft.applicationinsights.internal.channel;

import com.microsoft.applicationinsights.internal.channel.common.Transmission;

import java.util.concurrent.TimeUnit;

/**
 * A dispatcher should know how and to whom to dispatch a {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
 *
 * Created by gupele on 12/18/2014.
 */
public interface TransmissionDispatcher {
    void dispatch(Transmission transmission);

    void stop(long timeout, TimeUnit timeUnit);
}

