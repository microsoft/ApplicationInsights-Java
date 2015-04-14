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
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;

public final class TelemetryBufferTest {
    private static class MockSender implements TelemetriesTransmitter {
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

        public MockSender setExpectedNumberOfScheduleSendCalls(int expectedNumberOfScheduleSendCalls) {
            this.expectedNumberOfScheduleSendCalls = expectedNumberOfScheduleSendCalls;
            return this;
        }

        @Override
        public boolean scheduleSend(final TelemetriesTransmitter.TelemetriesFetcher telemetriesFetcher, long value, TimeUnit timeUnit) {
            assertNotNull(telemetriesFetcher);

            int called = scheduleSendCallCounter.incrementAndGet();
            assertEquals(called, 1);

            assertEquals(timeUnit, TimeUnit.SECONDS);

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
            assertEquals("Wrong size of telemetries container", telemetries.size(), expectedTelemetriesNumberInSendNow);

            return true;
        }

        @Override
        public void stop(long timeout, TimeUnit timeUnit) {

        }

        public void waitForFinish(long timeToWaitInSeconds) {
            try {
                ScheduledSendResult result = queue.poll(timeToWaitInSeconds, TimeUnit.SECONDS);
                scheduler.shutdownNow();
                assertTrue(result.message, result.result);

                assertEquals("Wrong number of calls by timer", scheduleSendActualCallCounter.get(), expectedNumberOfScheduleSendCalls);
                assertEquals("Wrong number of calls of send now", sendNowCallCounter.get(), expectedNumberOfSendNowCalls);

                assertEquals("Wrong number of scheduled sends by the TransmissionBuffer", scheduleSendCallCounter.get(), 1);
                assertEquals("Wrong number of sending full buffers by the TransmissionBuffer", sendNowCallCounter.get(), expectedNumberOfSendNowCalls);
            } catch (InterruptedException e) {
                assertTrue(false);
            }
        }
    };

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferSizeSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, -1, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroBufferSizeSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 0, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferTimeoutSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 1, -20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroBufferTimeoutSenderIsSet() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 20, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNoSenderIsSet() throws Exception {
        TelemetryBuffer testedBuffer = new TelemetryBuffer(null, 20, 0);
    }

    @Test
    public void testAddOneTelemetry() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 128, 2);

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

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 2, 1200);

        for (int i = 0; i < 2; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        Mockito.verify(mockSender, Mockito.times(1)).scheduleSend((TelemetriesTransmitter.TelemetriesFetcher) any(), anyLong(), (TimeUnit) anyObject());
        Mockito.verify(mockSender, Mockito.times(1)).sendNow(anyCollectionOf(String.class));
    }


    @Test
    public void testSendReturnsFalseOnScheduleSend() throws Exception {
        class StubTelemetriesTransmitter implements TelemetriesTransmitter {
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
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 3, 1200);

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

        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 1, 1200);

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
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 10, 3);

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
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 10, 3);

        for (int i = 0; i < 10; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        mockSender.waitForFinish(6L);
    }

    @Test
    public void testFlushWithZero() throws Exception {
        TelemetriesTransmitter mockSender = Mockito.mock(TelemetriesTransmitter.class);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 10, 3);
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

    private void testFlushWithData(int expectedTelemetriesNumberInSendNow) {
        MockSender mockSender = new MockSender()
                .setExpectedNumberOfScheduleSendCalls(1)
                .setExpectedNumberOfSendNowCalls(1)
                .setExpectedTelemetriesNumberInScheduleSend(0)
                .setExpectedTelemetriesNumberInSendNow(expectedTelemetriesNumberInSendNow);

        // Create a buffer with max buffer size of 10 and timeout of 10 seconds
        TelemetryBuffer testedBuffer = new TelemetryBuffer(mockSender, 10, 3);

        for (int i = 0; i < expectedTelemetriesNumberInSendNow; ++i) {
            testedBuffer.add("mockTelemetry");
        }

        testedBuffer.flush();

        mockSender.waitForFinish(6L);
    }
}
