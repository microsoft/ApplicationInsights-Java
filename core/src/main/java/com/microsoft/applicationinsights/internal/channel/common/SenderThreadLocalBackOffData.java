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

import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Preconditions;

/**
 * The class is responsible for managing the back-off policy of a Sender thread.
 *
 * To make sure the network part of the Channel works as expected, please make sure that:
 *
 * 1. The class is used by every Sending thread.
 * 2. Every time a send is done, the caller thread must report the outcome.
 * 3. The class should be 'attached' to a Sending thread, which should be the only one
 *    to access its 'backOff' method.
 *
 * Created by gupele on 2/9/2015.
 */
final class SenderThreadLocalBackOffData {
    private final ReentrantLock lock;
    private int currentBackOffIndex;
    private boolean instanceIsActive;
    private final long addMilliseconds;
    private final long[] backOffTimeoutsInMillis;

    /**
     * The constructor must get the {@link BackOffTimesPolicy} that will supply the needed back-off timeouts.
     * @param backOffTimeoutsInMillis The array of timeouts that will be used when the thread needs to back off.
     * @param addMilliseconds The amount of seconds that will be added to the 'large' intervals to distinct between sender threads.
     */
    public SenderThreadLocalBackOffData(long[] backOffTimeoutsInMillis, long addMilliseconds) {
        Preconditions.checkNotNull(backOffTimeoutsInMillis, "backOffTimeoutsInMillis must be not null");
        Preconditions.checkArgument(backOffTimeoutsInMillis.length > 0, "backOffTimeoutsInMillis must not be empty");
        Preconditions.checkArgument(addMilliseconds >= 0, "addMilliseconds must not be >= 0");

        currentBackOffIndex = -1;
        instanceIsActive = true;
        lock = new ReentrantLock();
        this.backOffTimeoutsInMillis = backOffTimeoutsInMillis;
        this.addMilliseconds = addMilliseconds;
    }

    public boolean isTryingToSend() {
        return currentBackOffIndex != -1;
    }

    /**
     * This method should be called by the Sender thread when the
     * Transmission is considered as 'done sending', which means the
     * Sender either sent the Transmission successfully or wishes to abandon its sending
     */
    public void onDoneSending() {
        currentBackOffIndex = -1;
    }

    /**
     * Increment the current back off amount or resets the counter if needed.
     * <p>
     * This method does not block but instead provides the amount of time to sleep which can be used
     * in another method.
     * @return The number of milliseconds to sleep for.
     */
    public long backOffTimerValue() {
        try {
            lock.lock();
            // when the last backoff index is hit, stay there until backoff is reset
            currentBackOffIndex = Math.min(currentBackOffIndex + 1, backOffTimeoutsInMillis.length - 1);

            if (!instanceIsActive) {
               return 0;
           }


                long millisecondsToWait = backOffTimeoutsInMillis[currentBackOffIndex];
                if (millisecondsToWait > BackOffTimesPolicy.MIN_TIME_TO_BACK_OFF_IN_MILLS) {
                    millisecondsToWait += addMilliseconds;
                }
                return millisecondsToWait;

       } finally {
           lock.unlock();
       }
   }
}
