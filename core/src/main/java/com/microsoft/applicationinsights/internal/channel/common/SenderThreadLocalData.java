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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

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
final class SenderThreadLocalData {
    private final ReentrantLock lock;
    private final Condition backOffCondition;
    private int currentBackOffIndex;
    private boolean instanceIsActive;
    private final int addSeconds;
    private final long[] backOffTimeoutsInSeconds;

    /**
     * The constructor must get the {@link BackOffTimesContainer} that will supply the needed back-off timeouts.
     * @param backOffTimeoutsInSeconds The array of timeouts that will be used when the thread needs to back off.
     * @param addSeconds The amount of seconds that will be added to the 'large' intervals to distinct between sender threads.
     */
    public SenderThreadLocalData(long[] backOffTimeoutsInSeconds, int addSeconds) {
        Preconditions.checkNotNull(backOffTimeoutsInSeconds, "backOffTimeoutsInSeconds must be not null");
        Preconditions.checkArgument(backOffTimeoutsInSeconds.length > 0, "backOffTimeoutsInSeconds must not be empty");
        Preconditions.checkArgument(addSeconds >= 0, "addSeconds must not be >= 0");

        currentBackOffIndex = -1;
        instanceIsActive = true;
        lock = new ReentrantLock();
        backOffCondition = lock.newCondition();
        this.backOffTimeoutsInSeconds = backOffTimeoutsInSeconds;
        this.addSeconds = addSeconds;
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
     * The calling thread will be suspended for an amount of time that is
     * set in the 'backOffTimeoutsInSeconds' array by using its index 'currentBackOffIndex'.
     *
     * @return True if the thread completed the suspension time as expected, in which case
     *         the caller should re-try to send the Transmission.
     *         False, in which case the caller should 'abandon' this Transmission and move to the next one,
     *         if:
     *         1. The instance was marked as non-active before the thread started to wait.
     *         2. The thread was signaled to stop while was waiting.
     *         3. The thread was interrupted while was waiting.
     *         4. The thread has exhausted all the back-off timeouts
     */
    public boolean backOff() {
         try {
             lock.lock();
             ++currentBackOffIndex;
             if (currentBackOffIndex == backOffTimeoutsInSeconds.length) {
                 InternalLogger.INSTANCE.trace("loosing");
                 currentBackOffIndex = -1;

                 // Exhausted the back-offs
                 return false;
             }

             if (!instanceIsActive) {
                return false;
            }

             try {
                 long secondsToWait = backOffTimeoutsInSeconds[currentBackOffIndex];
                 if (secondsToWait > BackOffTimesContainer.MIN_TIME_TO_BACK_OFF_IN_MILLS) {
                     secondsToWait += addSeconds;
                 }
                 InternalLogger.INSTANCE.trace("waiting %d", secondsToWait);
                 backOffCondition.await(secondsToWait, TimeUnit.SECONDS);
                 return instanceIsActive;
            } catch (InterruptedException e) {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stop a waiting thread if there is one, and prevent that thread for backOffing.
     */
    public void stop() {
        try {
            lock.lock();
            instanceIsActive = false;
            backOffCondition.signal();
        } finally {
            lock.unlock();
        }
    }
}
