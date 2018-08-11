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

package com.microsoft.applicationinsights.internal.channel;

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
public interface TelemetriesTransmitter<T> {
    public interface TelemetriesFetcher<T> {
        Collection<T> fetch();
    }

    boolean scheduleSend(TelemetriesFetcher telemetriesFetcher, long value, TimeUnit timeUnit);

    boolean sendNow(Collection<T> telemetries);

    void stop(long timeout, TimeUnit timeUnit);
}
