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

package com.microsoft.applicationinsights.internal.perfcounter;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
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

    @Before
    public void clear() {
        PerformanceCounterContainer.INSTANCE.clear();
    }

    @Test
    public void testSetStartCollectingIntervalInMillisNegativeNumber() {
        PerformanceCounterContainer.INSTANCE.setStartCollectingDelayInMillis(-100);
        assertTrue(PerformanceCounterContainer.INSTANCE.getStartCollectingDelayInMillis() > 0);
    }

    @Test
    public void testSetStartCollectingIntervalInMillisZero() {
        PerformanceCounterContainer.INSTANCE.setStartCollectingDelayInMillis(0);
        assertTrue(PerformanceCounterContainer.INSTANCE.getStartCollectingDelayInMillis() > 0);
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
    public void testRegisterWithNullId() throws NoSuchFieldException, IllegalAccessException {
        testBadPerformanceCounterId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWithEmptyId() throws NoSuchFieldException, IllegalAccessException {
        testBadPerformanceCounterId("");
    }

    @Test
    public void testOnePerformanceCounterCalls() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        initialize();
        PerformanceCounterStub mockPerformanceCounter = new PerformanceCounterStub("mock1");

        PerformanceCounterContainer.INSTANCE.register(mockPerformanceCounter);

        waitForFirstAndStop(mockPerformanceCounter, null);

        assertTrue(String.format("Counter is %d while was expected to be 2", mockPerformanceCounter.counter), mockPerformanceCounter.counter > 2);
    }

//    @Test
//    public void testTwoPerformanceCountersCalls() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
//        initialize();
//        PerformanceCounterStub mockPerformanceCounter1 = new PerformanceCounterStub("mock1");
//        PerformanceCounterStub mockPerformanceCounter2 = new PerformanceCounterStub("mock2");
//
//        registerInContainer(mockPerformanceCounter1, mockPerformanceCounter2);
//
//        waitForFirstAndStop(mockPerformanceCounter1, null);
//
//        assertTrue(String.format("Found %s for counter1", mockPerformanceCounter1.counter), mockPerformanceCounter1.counter > 1);
//        assertTrue(String.format("Found %s for counter2", mockPerformanceCounter2.counter), mockPerformanceCounter2.counter > 1);
//    }
//
    @Test
    public void testMoreThanOneWithId() throws NoSuchFieldException, IllegalAccessException {
        initialize();
        PerformanceCounterStub mockPerformanceCounter1 = new PerformanceCounterStub("mock1");
        PerformanceCounterStub mockPerformanceCounter2 = new PerformanceCounterStub("mock1");

        boolean result = PerformanceCounterContainer.INSTANCE.register(mockPerformanceCounter1);
        assertTrue(result);
        result = PerformanceCounterContainer.INSTANCE.register(mockPerformanceCounter2);
        assertFalse(result);
    }

    @Test
    public void testTwoPerformanceCountersWhereTheSecondIsNotRegistered() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        initialize(500);
        PerformanceCounterStub mockPerformanceCounter1 = new PerformanceCounterStub("mock1");
        PerformanceCounterStub mockPerformanceCounter2 = new PerformanceCounterStub("mock1");

        registerInContainer(mockPerformanceCounter1, mockPerformanceCounter2);

        waitForFirstAndStop(mockPerformanceCounter1, null);

        assertTrue(mockPerformanceCounter2.counter == 0);
        assertTrue(mockPerformanceCounter1.counter > 2);
    }

    @Test
    public void testTwoPerformanceCountersWhereTheSecondIsUnregistered() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
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

    private static void testBadPerformanceCounterId(String badId) throws NoSuchFieldException, IllegalAccessException {
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

    private static void initialize() throws NoSuchFieldException, IllegalAccessException {
        initialize(2200);
    }

    private static void initialize(long initialInterval) throws NoSuchFieldException, IllegalAccessException {
        Field field = PerformanceCounterContainer.class.getDeclaredField("startCollectingDelayInMillis");
        field.setAccessible(true);
        field.set(PerformanceCounterContainer.INSTANCE, new Long(initialInterval));

        field = PerformanceCounterContainer.class.getDeclaredField("collectingIntervalInMillis");
        field.setAccessible(true);
        field.set(PerformanceCounterContainer.INSTANCE, new Long(200));
    }
}
