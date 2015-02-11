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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public final class SenderThreadsBackOffManagerTest {
    private static class TimeMeasure {
        private Long start;
        private int elapsed;
        public void start() {} {
            if (start == null) {
                synchronized (this) {
                    if (start == null) {
                        start = new Long(System.nanoTime());
                    }
                }
            }
        }

        public int stop() {
            elapsed = (int)((double)(System.nanoTime() - start) / 1000000000.0);
            return elapsed;
        }
    }

    @Test
    public void testOneBackOff() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1});
        verifyBackOff(manager, 1, 1, true);
    }

    @Test
    public void testTwoBackOffs() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1, 2});
        verifyBackOff(manager, 2, 3, true, true);
    }

    @Test
    public void testExhaustedBackOffsWithOne() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1});
        verifyBackOff(manager, 2, 1, true, false);
    }

    @Test
    public void testExhaustedBackOffsWithTwo() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1, 1});
        verifyBackOff(manager, 3, 2, true, true, false);
    }

    @Test
    public void testExhaustedBackOffsWithOneAndRestarted() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1});
        verifyBackOff(manager, 3, 2, true, false, true);
    }

    @Test
    public void testExhaustedBackOffsWithTwoAndRestarted() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1, 1});
        verifyBackOff(manager, 4, 3, true, true, false, true);
    }

    @Test
    public void testOneThreadStoppedBeforeBackOff() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {5});
        verifyMultipleThreadsStoppedBeforeBackOff(manager, 1, 2);
    }

    @Test
    public void testTwoThreadsStoppedBeforeBackOff() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {5});
        verifyMultipleThreadsStoppedBeforeBackOff(manager, 2, 2);
    }

    @Test
    public void testSevenThreadsStoppedBeforeBackOff() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[]{5});
        verifyMultipleThreadsStoppedBeforeBackOff(manager, 7, 2);
    }

    @Test
    public void testTwoBackOffsAndDoneSendingInTheMiddle() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1, 5});
        verifyBackOffWithDoneSending(manager, 2, 2, 1, true, true);
    }

    @Test
    public void testTwoBackOffsAndDoneSendingBefore() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1, 1});
        verifyBackOffWithDoneSending(manager, 2, 2, 0, true, true);
    }

    @Test
    public void testTwoBackOffsAndDoneSendingAfterLast() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1, 1});
        verifyBackOffWithDoneSending(manager, 2, 2, 0, true, true);
    }

    @Test
    public void testTwoBackOffsAndDoneSendingAfterFirstOfNewRound() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {1, 2});
        verifyBackOffWithDoneSending(manager, 5, 6, 3, true, true, false, true, true);
    }

    @Test
    public void testStopWhileWaiting() throws InterruptedException {
        SenderThreadsBackOffManager manager = createManager(new long[] {5, 2});
        verifyMultipleThreadsStoppedWhileBackOff(manager, 1, 0);
    }

    private static SenderThreadsBackOffManager createManager(long[] backOffTimeouts) {
        BackOffTimesContainer container = Mockito.mock(BackOffTimesContainer.class);
        Mockito.doReturn(backOffTimeouts).when(container).getBackOffTimeoutsInSeconds();
        SenderThreadsBackOffManager manager = new SenderThreadsBackOffManager(container);

        return manager;
    }

    private static void verifyBackOff(SenderThreadsBackOffManager manager,
                                      int backOffTimes,
                                      int expectedSeconds,
                                      boolean... expectedBackOffResult) throws InterruptedException {
        verifyBackOffWithDoneSending(manager, backOffTimes, expectedSeconds, null, expectedBackOffResult);
    }

    private static void verifyBackOffWithDoneSending(SenderThreadsBackOffManager manager,
                                                     int backOffTimes,
                                                     int expectedSeconds,
                                                     Integer doneSendingAfter,
                                                     boolean... expectedBackOffResult) throws InterruptedException {
        int limit = backOffTimes + (doneSendingAfter == null ? 0 : 1);
        int reduce = 0;
        long start = new Long(System.nanoTime());
        for (int i = 0; i < limit; ++i) {
            if (doneSendingAfter != null && i == doneSendingAfter) {
                manager.onDoneSending();
                reduce = 1;
                continue;
            }
            boolean done = manager.backOffCurrentSenderThread();
            assertEquals(done, expectedBackOffResult[i - reduce]);
        }

        int elapsed = (int)((double)(System.nanoTime() - start) / 1000000000.0);
        assertTrue(String.format("BackOff lasted %d which is less than expected %d", elapsed, expectedSeconds), elapsed >= expectedSeconds);
        assertTrue(String.format("BackOff lasted %d which is more than expected %d", elapsed, expectedSeconds), elapsed <= expectedSeconds + 2);
    }

    private static void verifyMultipleThreadsStoppedBeforeBackOff(SenderThreadsBackOffManager manager,
                                                                  int numberOfThreads,
                                                                  int expectedMaxSeconds) throws InterruptedException {
        verifyMultipleThreadsWithStopBackOff(manager, numberOfThreads, expectedMaxSeconds, true);
    }

    private static void verifyMultipleThreadsStoppedWhileBackOff(SenderThreadsBackOffManager manager,
                                                                 int numberOfThreads,
                                                                 int expectedMaxSeconds) throws InterruptedException {
        verifyMultipleThreadsWithStopBackOff(manager, numberOfThreads, expectedMaxSeconds, false);
    }

    private static void verifyMultipleThreadsWithStopBackOff(final SenderThreadsBackOffManager manager,
                                                             int numberOfThreads,
                                                             int expectedMaxSeconds,
                                                             boolean stopBefore) throws InterruptedException {
        final Thread[] threads = new Thread[numberOfThreads];
        final CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        final TimeMeasure measure = new TimeMeasure();
        for (int i = 0; i < numberOfThreads; ++i) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                        measure.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                    manager.backOffCurrentSenderThread();
                }
            });
            thread.setDaemon(true);
            threads[i] = thread;
        }

        if (stopBefore) {
            manager.stopAllSendersBackOffActivities();
        }
        for (Thread thread : threads) {
            thread.start();
        }
        if (!stopBefore) {
            Thread.sleep(1000);
            ++expectedMaxSeconds;
            manager.stopAllSendersBackOffActivities();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        int elapsed = measure.stop();

        assertTrue(String.format("BackOff lasted %d which is more than expected %d", elapsed, expectedMaxSeconds), elapsed <= expectedMaxSeconds + 2);
    }
}
