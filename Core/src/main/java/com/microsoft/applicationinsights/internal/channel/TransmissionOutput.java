package com.microsoft.applicationinsights.internal.channel;

import com.microsoft.applicationinsights.internal.channel.common.Transmission;

import java.util.concurrent.TimeUnit;

/**
 * Defines the interface of classes that get a {@link com.microsoft.applicationinsights.internal.channel.common.Transmission}
 * and can 'send' it.
 *
 * Concrete classes can 'send' the data to remote server, to disk, database etc.
 *
 * Created by gupele on 12/18/2014.
 */
public interface TransmissionOutput {
    boolean send(Transmission transmission);

    void stop(long timeout, TimeUnit timeUnit);
}

