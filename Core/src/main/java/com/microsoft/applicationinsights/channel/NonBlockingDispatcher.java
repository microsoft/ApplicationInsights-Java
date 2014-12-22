package com.microsoft.applicationinsights.channel;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

/**
 * The class implements {@link com.microsoft.applicationinsights.channel.TransmissionDispatcher}
 *
 * Basically, the class tries to find one {@link com.microsoft.applicationinsights.channel.TransmissionOutput}
 * that will accept the incoming {@line Transmission}.
 *
 * It is a non blocking behavior in the sense that if no one can accept it will drop the data
 *
 * Created by gupele on 12/18/2014.
 */
public final class NonBlockingDispatcher implements TransmissionDispatcher {
    private final TransmissionOutput[] transmissionOutputs;

    public NonBlockingDispatcher(TransmissionOutput[] transmissionOutputs) {
        Preconditions.checkNotNull(transmissionOutputs, "transmissionOutputs should be non-null value");
        Preconditions.checkArgument(transmissionOutputs.length > 0, "There should be at least one TransmissionOutput");

        this.transmissionOutputs = transmissionOutputs;
    }

    @Override
    public void dispatch(Transmission transmission) {
        Preconditions.checkNotNull(transmission, "transmission should be non-null value");

        for (TransmissionOutput output : transmissionOutputs) {
            if (output.send(transmission)) {
                return;
            }
        }
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        for (TransmissionOutput output : transmissionOutputs) {
            output.stop(timeout, timeUnit);
        }
    }
}

