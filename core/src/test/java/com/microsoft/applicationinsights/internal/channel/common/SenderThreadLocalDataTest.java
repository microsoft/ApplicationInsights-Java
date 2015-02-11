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

import org.junit.Test;

import static org.junit.Assert.*;

public final class SenderThreadLocalDataTest {
    @Test(expected = NullPointerException.class)
    public void testNotSupplyingBackOffTimesContainer() {
        new SenderThreadLocalData(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithEmptyBackOffTimesContainer() {
        new SenderThreadLocalData(new long[]{}, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithEmptyNegativeAddSeconds() {
        new SenderThreadLocalData(new long[]{1}, -1);
    }

    @Test
    public void testStateAfterCtor() {
        final SenderThreadLocalData sender = createSenderThreadLocalData(new long[] {1});

        assertFalse(sender.isTryingToSend());
    }

    @Test
    public void testMultipleOnFailedSending() {
        final SenderThreadLocalData sender = createSenderThreadLocalData(new long[] {1,2,1});
        verifyBackOff(sender, 3, 4);
    }

    @Test
    public void testOnDoneSending() {
        final SenderThreadLocalData sender = createSenderThreadLocalData(new long[] {1});
        verifyOnDoneSending(sender);
    }

    @Test
    public void testDoneSendingAfterFailedSending() {
        final SenderThreadLocalData sender = createSenderThreadLocalData(new long[]{1});
        verifyBackOff(sender, 1, 1);
        verifyOnDoneSending(sender);
    }

    @Test
    public void testStopWhileWaiting() throws InterruptedException {
        final SenderThreadLocalData sender = createSenderThreadLocalData(new long[]{10});
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                sender.stop();
            }
        });
        thread.setDaemon(true);
        thread.start();
        sender.backOff();
        verifyOnDoneSending(sender);
        thread.join();
    }

    private SenderThreadLocalData createSenderThreadLocalData(long[] backOffs) {
        SenderThreadLocalData sender = new SenderThreadLocalData(backOffs, 0);
        return sender;
    }

    private static void verifyBackOff(SenderThreadLocalData sender, int backOffTimes, int expectedSeconds) {
        long started = System.nanoTime();
        for (int i = 0; i < backOffTimes; ++i) {
            sender.backOff();
        }

        int elapsed = (int)((double)(System.nanoTime() - started) / 1000000000.0);
        assertTrue(String.format("BackOff lasted %d which is less than expected %d", elapsed, expectedSeconds), elapsed >= expectedSeconds);
        assertTrue(String.format("BackOff lasted %d which is more than expected %d", elapsed, expectedSeconds), elapsed <= expectedSeconds + 2);
        assertTrue(sender.isTryingToSend());
    }

    private static void verifyOnDoneSending(SenderThreadLocalData sender) {
        sender.onDoneSending();

        assertFalse(sender.isTryingToSend());
    }
}
