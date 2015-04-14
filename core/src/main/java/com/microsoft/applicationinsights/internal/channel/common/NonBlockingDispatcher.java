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

import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;

/**
 * The class implements {@link TransmissionDispatcher}
 *
 * Basically, the class tries to find one {@link TransmissionOutput}
 * that will accept the incoming {@link Transmission}.
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

