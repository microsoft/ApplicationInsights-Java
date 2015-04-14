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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
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
        TransmissionOutput mock = new TransmissionOutput() {
            private ReentrantLock lock = new ReentrantLock();
            private Condition stopCondition = lock.newCondition();
            private boolean done = false;
            private AtomicInteger counter = new AtomicInteger(0);

            @Override
            public boolean send(Transmission transmission) {
                try {
                    counter.incrementAndGet();
                    lock.lock();
                    while (!done) {
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

            @Override
            public void stop(long timeout, TimeUnit timeUnit) {
                try {
                    lock.lock();
                    done = true;
                    stopCondition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };
        ActiveTransmissionNetworkOutput tested = new ActiveTransmissionNetworkOutput(mock, 1);
        numberExpected[0] = tested.getNumberOfMaxThreads();
        testSend(100, 1, tested);
        assertTrue("Too many calls to send", isError[0]);
    }

    private void testSend(int amount) throws InterruptedException {
        testSend(amount, amount, null);
    }

    private void testSend(int amount, int expectedSends, ActiveTransmissionNetworkOutput theTested) throws InterruptedException {
        TransmissionOutput mockOutput = null;
        ActiveTransmissionNetworkOutput tested = null;
        if (theTested == null) {
            mockOutput = Mockito.mock(TransmissionOutput.class);
            Mockito.doReturn(true).when(mockOutput).send((Transmission) anyObject());
            tested = new ActiveTransmissionNetworkOutput(mockOutput);
        } else {
            tested = theTested;
        }

        for (int i = 0; i < amount; ++i) {
            tested.send(new Transmission(new byte[2], MOCK_CONTENT_TYPE, MOCK_ENCODING_TYPE));
        }

        int waitCounter = 0;
        if (mockOutput != null) {
            try {
                Mockito.verify(mockOutput, Mockito.times(expectedSends)).send((Transmission) anyObject());
                Thread.sleep(1000);
            } catch (Error e) {
                ++waitCounter;
                if (waitCounter == 2) {
                    assertFalse(true);
                }
            }
        }

        tested.stop(60L, TimeUnit.SECONDS);
    }

    @Test
    public void testStop() throws Exception {
    }
}