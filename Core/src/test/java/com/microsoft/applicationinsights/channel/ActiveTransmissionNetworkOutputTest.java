package com.microsoft.applicationinsights.channel;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

import org.mockito.Mockito;

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

        if (mockOutput != null) {
            Thread.sleep(2000);
            Mockito.verify(mockOutput, Mockito.times(expectedSends)).send((Transmission) anyObject());
        }

        tested.stop(60L, TimeUnit.SECONDS);
    }

    @Test
    public void testStop() throws Exception {
    }
}