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

package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.microsoft.applicationinsights.TelemetryClient;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;

public final class PerformanceCounterContainerTest {
    private static class PerformanceCounterStub implements PerformanceCounter {
        private final String id;
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        private final long waitAfterFirst;
        public int counter = 0;

        private PerformanceCounterStub(String id) {
            this(id, 0);
        }

        private PerformanceCounterStub(String id, long waitAfterFirst) {
            this.id = id;
            this.waitAfterFirst = waitAfterFirst;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void report(TelemetryClient telemetryClient) {
            ++counter;
            if (counter > 1) {
                return;
            }

            try {
                lock.lock();
                condition.signal();
            } finally {
                lock.unlock();
            }
            if (waitAfterFirst > 0) {
                try {
                    Thread.sleep(waitAfterFirst);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void waitForFirstReportCall(long waitForInMillis) {
            try {
                lock.lock();
                condition.await(waitForInMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            } finally {
                lock.unlock();
            }
        }
    }

    @Test
    public void testSetStartCollectingIntervalInMillisNegativeNumber() {
        PerformanceCounterContainer.INSTANCE.setStartCollectingIntervalInMillis(-100);
        assertTrue(PerformanceCounterContainer.INSTANCE.getStartCollectingIntervalInMillis() > 0);
    }

    @Test
    public void testSetStartCollectingIntervalInMillisZero() {
        PerformanceCounterContainer.INSTANCE.setStartCollectingIntervalInMillis(0);
        assertTrue(PerformanceCounterContainer.INSTANCE.getStartCollectingIntervalInMillis() > 0);
    }

    @Test
    public void testSetCollectingIntervalInMillisMillisNegativeNumber() {
        PerformanceCounterContainer.INSTANCE.setCollectingIntervalInMillis(-100);
        assertTrue(PerformanceCounterContainer.INSTANCE.getCollectingIntervalInMillis() > 0);
    }

    @Test
    public void testSetCollectingIntervalInMillisZero() {
        PerformanceCounterContainer.INSTANCE.setCollectingIntervalInMillis(0);
        assertTrue(PerformanceCounterContainer.INSTANCE.getCollectingIntervalInMillis() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWithNullId() {
        testBadPerformanceCounterId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWithEmptyId() {
        testBadPerformanceCounterId("");
    }

    @Test
    public void testOnePerformanceCounterCalls() throws InterruptedException {
        initialize();
        PerformanceCounterStub mockPerformanceCounter = new PerformanceCounterStub("mock1");

        PerformanceCounterContainer.INSTANCE.register(mockPerformanceCounter);

        waitForFirstAndStop(mockPerformanceCounter, null);

        assertTrue(mockPerformanceCounter.counter > 2);
    }

    @Test
    public void testTwoPerformanceCountersCalls() throws InterruptedException {
        initialize();
        PerformanceCounterStub mockPerformanceCounter1 = new PerformanceCounterStub("mock1");
        PerformanceCounterStub mockPerformanceCounter2 = new PerformanceCounterStub("mock2");

        registerInContainer(mockPerformanceCounter1, mockPerformanceCounter2);

        waitForFirstAndStop(mockPerformanceCounter1, null);

        assertTrue(mockPerformanceCounter1.counter > 2);
        assertTrue(mockPerformanceCounter2.counter > 2);
    }

    @Test
    public void testTwoPerformanceCountersWhereTheSecondReplacesTheFirst() throws InterruptedException {
        initialize(500);
        PerformanceCounterStub mockPerformanceCounter1 = new PerformanceCounterStub("mock1");
        PerformanceCounterStub mockPerformanceCounter2 = new PerformanceCounterStub("mock1");

        registerInContainer(mockPerformanceCounter1, mockPerformanceCounter2);

        waitForFirstAndStop(mockPerformanceCounter1, null);

        assertTrue(mockPerformanceCounter1.counter == 0);
        assertTrue(mockPerformanceCounter2.counter > 4);
    }

    @Test
    public void testTwoPerformanceCountersWhereTheSecondIsUnregistered() throws InterruptedException {
        initialize(500);
        PerformanceCounterStub mockPerformanceCounter1 = new PerformanceCounterStub("mock1", 200);
        PerformanceCounterStub mockPerformanceCounter2 = new PerformanceCounterStub("mock2");

        registerInContainer(mockPerformanceCounter1, mockPerformanceCounter2);

        waitForFirstAndStop(mockPerformanceCounter1, mockPerformanceCounter2);

        assertTrue(mockPerformanceCounter2.counter < mockPerformanceCounter1.counter);
    }

    private static void registerInContainer(PerformanceCounter... performanceCounters) {
        for (PerformanceCounter performanceCounter : performanceCounters) {
            PerformanceCounterContainer.INSTANCE.register(performanceCounter);
        }
    }

    private static void waitForFirstAndStop(PerformanceCounterStub mockPerformanceCounter, PerformanceCounterStub unregister) throws InterruptedException {
        mockPerformanceCounter.waitForFirstReportCall(1500);
        if (unregister != null) {
            PerformanceCounterContainer.INSTANCE.unregister(unregister);
        }
        Thread.sleep(500);
        PerformanceCounterContainer.INSTANCE.stop(1L, TimeUnit.SECONDS);
    }

    private static void testBadPerformanceCounterId(String badId) {
        initialize();
        PerformanceCounterContainer.INSTANCE.register(createMockPerformanceCounter(badId));
    }

    private static PerformanceCounter createMockPerformanceStub(final String id) {
        return new PerformanceCounter() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public void report(TelemetryClient telemetryClient) {
            }
        };
    }

    private static PerformanceCounter createMockPerformanceCounter(String id) {
        PerformanceCounter mockPerformanceCounter = Mockito.mock(PerformanceCounter.class);
        Mockito.doReturn(id).when(mockPerformanceCounter).getId();

        return mockPerformanceCounter;
    }

    private static void initialize() {
        initialize(100);
    }

    private static void initialize(long initialInterval) {
        PerformanceCounterContainer.INSTANCE.setStartCollectingIntervalInMillis(100);
        PerformanceCounterContainer.INSTANCE.setCollectingIntervalInMillis(200);
    }
}