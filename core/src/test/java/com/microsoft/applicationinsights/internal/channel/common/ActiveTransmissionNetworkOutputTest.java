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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.microsoft.applicationinsights.internal.channel.TransmissionOutputSync;
import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;

public class ActiveTransmissionNetworkOutputTest {
    private final static String MOCK_CONTENT_TYPE = "MockContentType";
    private final static String MOCK_ENCODING_TYPE = "MockContentType";

    @Test
    public void testSendOneTransmission() throws Exception {
        testSend(1);
    }

    @Test
    public void testSendTwoTransmission() throws Exception {
        testSend(2);
    }

    @Test
    public void testSendTenTransmission() throws Exception {
        testSend(10);
    }

    @Test
    public void testBufferIsFull() throws Exception {
        final boolean[] isError = {false};
        final int[] numberExpected = {0};
        final ReentrantLock lock = new ReentrantLock();
        final Condition stopCondition = lock.newCondition();
        final AtomicBoolean done = new AtomicBoolean();
        TransmissionOutputSync mock = new TransmissionOutputSync() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public boolean sendSync(Transmission transmission) {
                try {
                    counter.incrementAndGet();
                    lock.lock();
                    while (!done.get()) {
                        try {
                            stopCondition.await();

                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } finally {
                    lock.unlock();
                }

                // Current default max number of threads + 1
                if (counter.get() != numberExpected[0]) {
                    isError[0] = true;
                }
                return false;
            }
        };
        TransmissionPolicyStateFetcher mockStateFetcher = Mockito.mock(TransmissionPolicyStateFetcher.class);
        Mockito.doReturn(TransmissionPolicy.UNBLOCKED).when(mockStateFetcher).getCurrentState();

        ActiveTransmissionNetworkOutput tested = new ActiveTransmissionNetworkOutput(mock, mockStateFetcher, 1);
        numberExpected[0] = tested.getNumberOfMaxThreads();
        testSend(100, 1, tested);
        try {
            lock.lock();
            done.set(true);
            stopCondition.signalAll();
        } finally {
            lock.unlock();
        }
        tested.shutdown(60L, TimeUnit.SECONDS);
        assertTrue("Too many calls to send", isError[0]);
    }

    private void testSend(int amount) throws InterruptedException {
        testSend(amount, amount, null);
    }

    private void testSend(int amount, int expectedSends, ActiveTransmissionNetworkOutput theTested) throws InterruptedException {
        TransmissionOutputSync mockOutput = null;
        ActiveTransmissionNetworkOutput tested;
        if (theTested == null) {
            mockOutput = Mockito.mock(TransmissionOutputSync.class);
            Mockito.doReturn(true).when(mockOutput).sendSync(anyObject());

            TransmissionPolicyStateFetcher mockStateFetcher = Mockito.mock(TransmissionPolicyStateFetcher.class);
            Mockito.doReturn(TransmissionPolicy.UNBLOCKED).when(mockStateFetcher).getCurrentState();

            tested = new ActiveTransmissionNetworkOutput(mockOutput, mockStateFetcher);
        } else {
            tested = theTested;
        }

        for (int i = 0; i < amount; ++i) {
            tested.sendAsync(new Transmission(new byte[2], MOCK_CONTENT_TYPE, MOCK_ENCODING_TYPE));
        }

        int waitCounter = 0;
        if (mockOutput != null) {
            try {
                Mockito.verify(mockOutput, Mockito.times(expectedSends)).sendSync(anyObject());
                Thread.sleep(1000);
            } catch (Error e) {
                ++waitCounter;
                if (waitCounter == 2) {
                    assertFalse(true);
                }
            }
        }
    }

    @Test
    public void testStop() {
    }
}