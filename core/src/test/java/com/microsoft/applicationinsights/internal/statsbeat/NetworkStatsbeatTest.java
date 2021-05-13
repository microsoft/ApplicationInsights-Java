package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static org.junit.Assert.assertEquals;

public class NetworkStatsbeatTest {

    private NetworkStatsbeat networkStatsbeat;

    @Before
    public void init() {
        networkStatsbeat = new NetworkStatsbeat(new TelemetryClient(), DEFAULT_STATSBEAT_INTERVAL);
    }

    @Test
    public void testAddInstrumentation() {
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.jdbc");
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.tomcat-7.0");
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.http-url-connection");
        assertEquals((long)(Math.pow(2, 13) + Math.pow(2, 21) + Math.pow(2, 57)), networkStatsbeat.getInstrumentation()); // Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP.)
    }

    @Test
    public void testIncrementRequestSuccessCount() {
        assertEquals(0, networkStatsbeat.getRequestSuccessCount());
        assertEquals(0, networkStatsbeat.getRequestDurationAvg(), 0);
        networkStatsbeat.incrementRequestSuccessCount(1000);
        networkStatsbeat.incrementRequestSuccessCount(3000);
        assertEquals(2, networkStatsbeat.getRequestSuccessCount());
        assertEquals(2000.0, networkStatsbeat.getRequestDurationAvg(), 0);
    }

    @Test
    public void testIncrementRequestFailureCount() {
        assertEquals(0, networkStatsbeat.getRequestFailureCount());
        networkStatsbeat.incrementRequestFailureCount();
        networkStatsbeat.incrementRequestFailureCount();
        assertEquals(2, networkStatsbeat.getRequestFailureCount());
    }

    @Test
    public void testIncrementRetryCount() {
        assertEquals(0, networkStatsbeat.getRetryCount());
        networkStatsbeat.incrementRetryCount();
        networkStatsbeat.incrementRetryCount();
        assertEquals(2, networkStatsbeat.getRetryCount());
    }

    @Test
    public void testIncrementThrottlingCount() {
        assertEquals(0, networkStatsbeat.getThrottlingCount());
        networkStatsbeat.incrementThrottlingCount();
        networkStatsbeat.incrementThrottlingCount();
        assertEquals(2, networkStatsbeat.getThrottlingCount());
    }

    @Test
    public void testIncrementExceptionCount() {
        assertEquals(0, networkStatsbeat.getExceptionCount());
        networkStatsbeat.incrementExceptionCount();
        networkStatsbeat.incrementExceptionCount();
        assertEquals(2, networkStatsbeat.getExceptionCount());
    }

    @Test
    public void testInterval() {
        assertEquals(DEFAULT_STATSBEAT_INTERVAL, networkStatsbeat.getInterval());
    }

    @Test
    public void testRaceCondition() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(100);
        final AtomicInteger instrumentationCounter = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        networkStatsbeat.incrementRequestSuccessCount(j % 2 == 0 ? 5 : 10);
                        networkStatsbeat.incrementRequestFailureCount();
                        networkStatsbeat.incrementRetryCount();
                        networkStatsbeat.incrementThrottlingCount();
                        networkStatsbeat.incrementExceptionCount();
                        networkStatsbeat.addInstrumentation("instrumentation" + instrumentationCounter.getAndDecrement());
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        assertEquals(100000, networkStatsbeat.getRequestSuccessCount());
        assertEquals(100000, networkStatsbeat.getRequestFailureCount());
        assertEquals(100000, networkStatsbeat.getRetryCount());
        assertEquals(100000, networkStatsbeat.getThrottlingCount());
        assertEquals(100000, networkStatsbeat.getExceptionCount());
        assertEquals(7.5, networkStatsbeat.getRequestDurationAvg(), 0);
        assertEquals(100000, networkStatsbeat.getInstrumentationList().size());
    }
}
