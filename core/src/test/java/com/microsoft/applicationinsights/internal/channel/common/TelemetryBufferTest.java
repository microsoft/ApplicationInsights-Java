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

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.util.LimitsEnforcer;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;

public final class TelemetryBufferTest {
    private final static String MOCK_PROPERTY_NAME = "MockProperty";

    private static class MockSender implements TelemetriesTransmitter<String> {
        private static class ScheduledSendResult {
            public final boolean result;

            public final String message;

            private ScheduledSendResult(boolean result, String message) {
                this.result = result;
                this.message = message;
            }
        }

        private AtomicInteger sendNowCallCounter = new AtomicInteger(0);

        private AtomicInteger scheduleSendCallCounter = new AtomicInteger(0);

        private AtomicInteger scheduleSendActualCallCounter = new AtomicInteger(0);

        private int expectedTelemetriesNumberInSendNow;
        private int expectedTelemetriesNumberInScheduleSend;
        private int expectedNumberOfSendNowCalls;
        private int expectedNumberOfScheduleSendCalls;
        private int expectedNumberOfScheduleSendRequests = 1;

        private ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

        private final BlockingQueue<ScheduledSendResult> queue = new ArrayBlockingQueue<ScheduledSendResult>(4);

        public MockSender setExpectedTelemetriesNumberInSendNow(int expectedTelemetriesNumberInSendNow) {
            this.expectedTelemetriesNumberInSendNow = expectedTelemetriesNumberInSendNow;
            return this;
        }

        public MockSender setExpectedTelemetriesNumberInScheduleSend(int expectedTelemetriesNumberInScheduleSend) {
            this.expectedTelemetriesNumberInScheduleSend = expectedTelemetriesNumberInScheduleSend;
            return this;
        }

        public MockSender setExpectedNumberOfSendNowCalls(int expectedNumberOfSendNowCalls) {
            this.expectedNumberOfSendNowCalls = expectedNumberOfSendNowCalls;
            return this;
        }

        public MockSender setExpectedNumberOfScheduleSendRequests(int expectedNumberOfScheduleSendRequests) {
            this.expectedNumberOfScheduleSendRequests = expectedNumberOfScheduleSendRequests;
            return this;
        }

        public MockSender setExpectedNumberOfScheduleSendCalls(int expectedNumberOfScheduleSendCalls) {
            this.expectedNumberOfScheduleSendCalls = expectedNumberOfScheduleSendCalls;
            return this;
        }

        @Override
        public boolean scheduleSend(final TelemetriesTransmitter.TelemetriesFetcher telemetriesFetcher, long value, TimeUnit timeUnit) {
            assertNotNull(telemetriesFetcher);

            scheduleSendCallCounter.incrementAndGet();

            assertEquals(TimeUnit.SECONDS, timeUnit);

            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    scheduleSendActualCallCounter.incrementAndGet();

                    Collection<String> telemetries = telemetriesFetcher.fetch();
                    if (telemetries == null) {
                        queue.offer(new ScheduledSendResult(false, "Telemetries is null"));
                        return;
                    }

                    if (telemetries.size() != expectedTelemetriesNumberInScheduleSend) {
                        queue.offer(new ScheduledSendResult(false, "Telemetries size is wrong"));
                        return;
                    }

                    queue.offer(new ScheduledSendResult(true, ""));
                }
            }, value, timeUnit);

            return true;
        }

        @Override
        public boolean sendNow(Collection<String> telemetries) {
            int called = sendNowCallCounter.incrementAndGet();
            assertEquals("Wrong number of scheduled sends by the TransmissionBuffer", called, expectedNumberOfSendNowCalls);

            assertNotNull("Unexpected null value for telemetries container", telemetries);
            assertEquals("Wrong size of telemetries container", expectedTelemetriesNumberInSendNow, telemetries.size());

            return true;
        }

        @Override
        public void stop(long timeout, TimeUnit timeUnit) {

        }

        public void waitForFinish(long timeToWaitInSeconds) {
            try {
                ScheduledSendResult result = queue.poll(timeToWaitInSeconds, TimeUnit.SECONDS);
                scheduler.shutdownNow();

                assertEquals("Wrong number of calls by timer", scheduleSendActualCallCounter.get(), expectedNumberOfScheduleSendCalls);
                if (expectedNumberOfScheduleSendCalls == 0) {
                    assertNull("Result should be null", result);
                } else {
                    assertTrue(result.message, result.result);
                }
                assertEquals("Wrong number of calls of send now", sendNowCallCounter.get(), expectedNumberOfSendNowCalls);

                assertEquals("Wrong number of scheduled sends by the TransmissionBuffer", scheduleSendCallCounter.get(), expectedNumberOfScheduleSendRequests);
                assertEquals("Wrong number of sending full buffers by the TransmissionBuffer", sendNowCallCounter.get(), expectedNumberOfSendNowCalls);
            } catch (InterruptedException e) {
                assertTrue(false);
            }
        }
    };

    @Test(expected = NullPointerException.class)
    public void testNullMaxTelemetriesEnforcer() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        LimitsEnforcer sendEnforcer = LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 1, 200, 20, null);

        new TelemetryBuffer(mockSender, null, sendEnforcer);
    }

    @Test(expected = NullPointerException.class)
    public void testNullSenderTimeoutEnforcer() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        LimitsEnforcer maxEnforcer = createDefaultBatchSizeEnforcer();

        new TelemetryBuffer(mockSender, maxEnforcer, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferSizeSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(-1);
        LimitsEnforcer sendEnforcer = createDefaultSenderTimeoutEnforcer();

        new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroBufferSizeSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(0);
        LimitsEnforcer sendEnforcer = createDefaultSenderTimeoutEnforcer();

        new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferTimeoutSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        LimitsEnforcer maxEnforcer = createDefaultBatchSizeEnforcer();
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(-1);

        new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroBufferTimeoutSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        LimitsEnforcer maxEnforcer = createDefaultBatchSizeEnforcer();
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(0);

        new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);
    }

    @Test(expected = NullPointerException.class)
    public void testNoSenderIsSet() throws Exception {

        LimitsEnforcer maxEnforcer = createDefaultBatchSizeEnforcer();
        LimitsEnforcer sendEnforcer = createDefaultSenderTimeoutEnforcer();

        new TelemetryBuffer(null, maxEnforcer, sendEnforcer);
    }

    @Test
    public void testAddOneTelemetry() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);


        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(128);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(2);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        testedBuffer.add("mockTelemetry");

        Mockito.verify(mockSender, Mockito.times(1)).scheduleSend((TelemetriesTransmitter.TelemetriesFetcher) any(), anyLong(), (TimeUnit) anyObject());
    }

    // Ignore warning from mock
    @SuppressWarnings("unchecked")
    @Test
    public void testSendWhenBufferIsFullInNonDeveloperMode() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);
        Mockito.doReturn(true).when(mockSender).sendNow(anyCollection());
        Mockito.doReturn(true).when(mockSender).scheduleSend(any(TelemetriesTransmitter.TelemetriesFetcher.class), anyLong(), any(TimeUnit.class));

        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(2);
        LimitsEnforcer sendEnforcer = createDefaultSenderTimeoutEnforcer();

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 2; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        Mockito.verify(mockSender, Mockito.times(1)).scheduleSend((TelemetriesTransmitter.TelemetriesFetcher) any(), anyLong(), (TimeUnit) anyObject());
        Mockito.verify(mockSender, Mockito.times(1)).sendNow(anyCollectionOf(String.class));
    }


    @Test
    public void testSendReturnsFalseOnScheduleSend() throws Exception {
        class StubTelemetriesTransmitter implements TelemetriesTransmitter<String> {
            private int scheduleSendCounter = 2;
            private Collection<String> sendNowCollection;

            @Override
            public boolean scheduleSend(TelemetriesFetcher telemetriesFetcher, long value, TimeUnit timeUnit) {
                --scheduleSendCounter;
                if (scheduleSendCounter > 0) {
                    return false;
                }

                return true;
            }

            @Override
            public boolean sendNow(Collection<String> telemetries) {
                sendNowCollection = telemetries;
                return true;
            }

            @Override
            public void stop(long timeout, TimeUnit timeUnit) {

            }

            public Collection<String> getSendNowCollection() {
                return sendNowCollection;
            }
        };

        List<String> all = new ArrayList<String>();
        List<String> expected = new ArrayList<String>();
        for (int i = 0; i < 4; ++i) {
            String mockSerializedTelemetry = "mockTelemtry" + String.valueOf(i);
            all.add(mockSerializedTelemetry);

            if (i != 0) {
                expected.add(mockSerializedTelemetry);
            }
        }

        StubTelemetriesTransmitter mockSender = new StubTelemetriesTransmitter();
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(3);
        LimitsEnforcer sendEnforcer = createDefaultSenderTimeoutEnforcer();

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (String telemetry : all) {
            testedBuffer.add(telemetry);
        }

        Collection<String> sendNowCollection = mockSender.getSendNowCollection();
        assertEquals(sendNowCollection.size(), expected.size());

        int i = 0;
        for (String telemetry : sendNowCollection) {
            assertEquals(telemetry, expected.get(i));
            ++i;
        }
    }

    @Test
    public void testSendWhenBufferIsFullInDeveloperMode() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(1);
        LimitsEnforcer sendEnforcer = createDefaultSenderTimeoutEnforcer();

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 2; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        Mockito.verify(mockSender, Mockito.never()).scheduleSend((TelemetriesTransmitter.TelemetriesFetcher)any(), anyLong(), (TimeUnit)anyObject());
        Mockito.verify(mockSender, Mockito.times(2)).sendNow(anyCollectionOf(String.class));
    }

    @Test
    public void testSendBufferAfterTimeoutExpires() throws Exception {

        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(1)
                .setExpectedNumberOfSendNowCalls(0)
                .setExpectedTelemetriesNumberInScheduleSend(1)
                .setExpectedTelemetriesNumberInSendNow(1);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(3);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 1; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        mockSender.waitForFinish(6L);
    }

    @Test
    public void testSendBufferAfterTimeoutExpiresButBufferWasAlreadySent() throws Exception {
        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(1)
                .setExpectedNumberOfSendNowCalls(1)
                .setExpectedTelemetriesNumberInScheduleSend(0)
                .setExpectedTelemetriesNumberInSendNow(10);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(3);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 10; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        mockSender.waitForFinish(6L);
    }

    @Test
    public void testFlushWithZero() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(3);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);
        testedBuffer.flush();

        Mockito.verify(mockSender, Mockito.never()).sendNow(anyCollectionOf(String.class));
    }

    @Test
    public void testFlushWithOneInTheBuffer() throws Exception {
        testFlushWithData(1);
    }

    @Test
    public void testFlushWithSevenInTheBuffer() throws Exception {
        testFlushWithData(7);
    }

    @Test
    public void testSetTransmitBufferTimeoutInSecondsShorterTime() {
        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(0)
                .setExpectedNumberOfSendNowCalls(1)
                .setExpectedTelemetriesNumberInScheduleSend(0)
                .setExpectedTelemetriesNumberInSendNow(2);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(1, 30);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 2; ++i) {
            testedBuffer.add("mockTelemetry");
        }
        testedBuffer.setTransmitBufferTimeoutInSeconds(1);

        mockSender.waitForFinish(1L);
    }

    @Test
    public void testSetMaxTelemetriesInBatchWithSmallerSize() {
        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(0)
                .setExpectedNumberOfSendNowCalls(1)
                .setExpectedTelemetriesNumberInScheduleSend(0)
                .setExpectedTelemetriesNumberInSendNow(2);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(1, 10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(30);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 2; ++i) {
            testedBuffer.add("mockTelemetry");
        }
        testedBuffer.setMaxTelemetriesInBatch(1);

        mockSender.waitForFinish(1L);
    }

    @Test
    public void testSetMaxTelemetriesInBatchWithSmallerSizeButLargerThanWhatInBuffer() {
        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(0)
                .setExpectedNumberOfSendNowCalls(1)
                .setExpectedTelemetriesNumberInScheduleSend(0)
                .setExpectedTelemetriesNumberInSendNow(3)
                .setExpectedNumberOfScheduleSendRequests(2);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(1, 10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(30);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 2; ++i) {
            testedBuffer.add("mockTelemetry");
        }
        testedBuffer.setMaxTelemetriesInBatch(3);
        for (int i = 0; i < 2; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        mockSender.waitForFinish(1L);
    }

    @Test
    public void testSetMaxTelemetriesInBatchWithBiggerSize() {
        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(0)
                .setExpectedNumberOfSendNowCalls(1)
                .setExpectedTelemetriesNumberInScheduleSend(0)
                .setExpectedTelemetriesNumberInSendNow(11);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(1, 10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(30);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < 1; ++i) {
            testedBuffer.add("mockTelemetry");
        }
        testedBuffer.setMaxTelemetriesInBatch(11);
        for (int i = 0; i < 10; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        mockSender.waitForFinish(1L);
    }

    private void testFlushWithData(int expectedTelemetriesNumberInSendNow) {
        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(1)
                .setExpectedNumberOfSendNowCalls(1)
                .setExpectedTelemetriesNumberInScheduleSend(0)
                .setExpectedTelemetriesNumberInSendNow(expectedTelemetriesNumberInSendNow);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        LimitsEnforcer maxEnforcer = createEnforcerWithCurrentValue(1, 10);
        LimitsEnforcer sendEnforcer = createEnforcerWithCurrentValue(1, 3);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, maxEnforcer, sendEnforcer);

        for (int i = 0; i < expectedTelemetriesNumberInSendNow; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        testedBuffer.flush();

        mockSender.waitForFinish(6L);
    }

    private LimitsEnforcer createDefaultBatchSizeEnforcer() {
        return LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 1, 10000, 100, null);
    }

    private LimitsEnforcer createDefaultSenderTimeoutEnforcer() {
        return LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, 1, 12000, 1200, 1200);
    }

    private LimitsEnforcer createEnforcerWithCurrentValue(int minimum) {
        return createEnforcerWithCurrentValue(minimum, minimum);
    }

    private LimitsEnforcer createEnforcerWithCurrentValue(int minimum, int defaultValue) {
        return LimitsEnforcer.createWithClosestLimitOnError(MOCK_PROPERTY_NAME, minimum, 10000, defaultValue, null);
    }
}
