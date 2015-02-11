/*
 * AppInsights-Java
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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.google.common.primitives.Longs;

/**
 * The class is responsible for managing the back-offs of Sender Threads.
 *
 * Sending threads are expected to:
 * 1. Call 'backOffCurrentSenderThread' when they need to suspend their work with a Transmission.
 *    A typical scenario is when the thread was given a 'Throttled' response from the server.
 * 2. Call 'onDoneSending' when the thread doesn't need to re-send the Transmission. This might
 *    happen when the Transmission was successfully sent, or when there are errors that lead to this decision.
 *
 * In either way, each send activity must be followed by calling one of those methods. Failing to do so, might result in undefined behavior.
 *
 * Created by gupele on 2/9/2015.
 */
final class SenderThreadsBackOffManager extends ThreadLocal<SenderThreadLocalData> {
    // The back-off timeouts that will be used by sender threads when need to back-off.
    private long[] backOffTimeoutsInSeconds = null;

    // All the thread local data
    private final ArrayList<SenderThreadLocalData> allSendersData;

    // A way to distinct
    private final AtomicInteger threadsSecondsDifference = new AtomicInteger(-1);

    private SenderThreadLocalData senderThreadLocalData;
    private boolean stopped;

    public SenderThreadsBackOffManager(BackOffTimesContainer backOffTimesContainer) {
        allSendersData = new ArrayList<SenderThreadLocalData>();
        initializeBackOffTimeouts(backOffTimesContainer);
    }

    public void onDoneSending() {
        SenderThreadLocalData currentThreadData = this.get();
        currentThreadData.onDoneSending();
    }

    public boolean backOffCurrentSenderThread() {
        SenderThreadLocalData currentThreadData = this.get();
        return currentThreadData.backOff();
    }

    public synchronized void stopAllSendersBackOffActivities() {
        for (SenderThreadLocalData sender : allSendersData) {
            sender.stop();
        }
        stopped = true;
    }

    @Override
    protected SenderThreadLocalData initialValue() {
        int addSeconds = threadsSecondsDifference.incrementAndGet();
        senderThreadLocalData = new SenderThreadLocalData(backOffTimeoutsInSeconds, addSeconds);
        registerSenderData(senderThreadLocalData);
        return senderThreadLocalData;
    }

    private synchronized void registerSenderData(SenderThreadLocalData senderData) {
        if (stopped) {
            senderData.stop();
        }

        allSendersData.add(senderData);
    }

    /**
     * Initialize the 'backOffTimeoutsInSeconds' container, which should be done only once.
     * @param container The container that supplies the back-off timeouts in seconds.
     *                  Note that if the container returns null, an exception (NullPointerException) will be thrown.
     */
    private synchronized void initializeBackOffTimeouts(BackOffTimesContainer container) {
        if (backOffTimeoutsInSeconds != null) {
            return;
        }

        if (container == null) {
            backOffTimeoutsInSeconds = new ExponentialBackOffTimesContainer().getBackOffTimeoutsInSeconds();
            InternalLogger.INSTANCE.trace("No BackOffTimesContainer, using default values.");
            return;
        }

        long[] injectedBackOffTimeoutsInSeconds = container.getBackOffTimeoutsInSeconds();
        ArrayList<Long> validBackOffTimeoutsInSeconds = new ArrayList<Long>();
        if (injectedBackOffTimeoutsInSeconds != null) {
            for (long backOffValue : injectedBackOffTimeoutsInSeconds) {
                if (backOffValue <= 0) {
                    continue;
                }

                validBackOffTimeoutsInSeconds.add(backOffValue);
            }
        }

        if (validBackOffTimeoutsInSeconds.isEmpty()) {
            backOffTimeoutsInSeconds = new ExponentialBackOffTimesContainer().getBackOffTimeoutsInSeconds();
            InternalLogger.INSTANCE.trace("BackOff timeouts are not supplied or not valid, using default values.");
            return;
        }

        backOffTimeoutsInSeconds = Longs.toArray(validBackOffTimeoutsInSeconds);
    }
}
