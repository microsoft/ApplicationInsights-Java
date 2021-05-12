package com.microsoft.applicationinsights.internal.statsbeat;

import com.google.common.util.concurrent.AtomicDouble;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.FEATURE_STATSBEAT_INTERVAL;
import static org.junit.Assert.assertEquals;

public class NetworkStatsbeatTest {

    private NetworkStatsbeat networkStatsbeat;

    @Before
    public void init() {
        StatsbeatModule.getInstance().initialize(new TelemetryClient(), DEFAULT_STATSBEAT_INTERVAL, FEATURE_STATSBEAT_INTERVAL);
        networkStatsbeat = StatsbeatModule.getInstance().getNetworkStatsbeat();
    }

    @Test
    public void testAddInstrumentation() {
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.jdbc");
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.tomcat-7.0");
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.http-url-connection");
        assertEquals(networkStatsbeat.getInstrumentation(), (long)(Math.pow(2, 13) + Math.pow(2, 21) + Math.pow(2, 57))); // 2^13 + 2^21 + 2^57 (Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP.)
    }

    @Test
    public void testIncrementRequestSuccessCount() {
        assertEquals(networkStatsbeat.getRequestSuccessCount(), 0);
        networkStatsbeat.incrementRequestSuccessCount();
        networkStatsbeat.incrementRequestSuccessCount();
        assertEquals(networkStatsbeat.getRequestSuccessCount(), 2);
    }

    @Test
    public void testIncrementRequestFailureCount() {
        assertEquals(networkStatsbeat.getRequestFailureCount(), 0);
        networkStatsbeat.incrementRequestFailureCount();
        networkStatsbeat.incrementRequestFailureCount();
        assertEquals(networkStatsbeat.getRequestFailureCount(), 2);
    }

    @Test
    public void testAddRequestDuration() {
        assertEquals(networkStatsbeat.getRequestDurationCount(), 0);
        networkStatsbeat.addRequestDuration(1000);
        networkStatsbeat.addRequestDuration(3000);
        assertEquals(networkStatsbeat.getRequestDurationCount(), 2);
        assertEquals(networkStatsbeat.getRequestDurationAvg(networkStatsbeat.getIntervalMetrics()), 2000.0, 0);
    }

    @Test
    public void testIncrementRetryCount() {
        assertEquals(networkStatsbeat.getRetryCount(), 0);
        networkStatsbeat.incrementRetryCount();
        networkStatsbeat.incrementRetryCount();
        assertEquals(networkStatsbeat.getRetryCount(), 2);
    }

    @Test
    public void testIncrementThrottlingCount() {
        assertEquals(networkStatsbeat.getThrottlingCount(), 0);
        networkStatsbeat.incrementThrottlingCount();
        networkStatsbeat.incrementThrottlingCount();
        assertEquals(networkStatsbeat.getThrottlingCount(), 2);
    }

    @Test
    public void testIncrementExceptionCount() {
        assertEquals(networkStatsbeat.getExceptionCount(), 0);
        networkStatsbeat.incrementExceptionCount();
        networkStatsbeat.incrementExceptionCount();
        assertEquals(networkStatsbeat.getExceptionCount(), 2);
    }

    @Test
    public void testInterval() {
        assertEquals(networkStatsbeat.getInterval(), DEFAULT_STATSBEAT_INTERVAL);
    }

    @Test
    public void testRaceCondition() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(100);
        final AtomicDouble durationCounter = new AtomicDouble();
        final AtomicInteger instrumentationCounter = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        networkStatsbeat.incrementRequestSuccessCount();
                        networkStatsbeat.incrementRequestFailureCount();
                        networkStatsbeat.incrementRetryCount();
                        networkStatsbeat.incrementThrottlingCount();
                        networkStatsbeat.incrementExceptionCount();
                        networkStatsbeat.addRequestDuration(durationCounter.getAndAdd(0.5));
                        networkStatsbeat.addInstrumentation("instrumentation" + instrumentationCounter.getAndDecrement());
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        assertEquals(networkStatsbeat.getRequestSuccessCount(), 100000);
        assertEquals(networkStatsbeat.getRequestFailureCount(), 100000);
        assertEquals(networkStatsbeat.getRetryCount(), 100000);
        assertEquals(networkStatsbeat.getThrottlingCount(), 100000);
        assertEquals(networkStatsbeat.getExceptionCount(), 100000);
        assertEquals(networkStatsbeat.getRequestDurationCount(), 100000);
        assertEquals(networkStatsbeat.getInstrumentationList().size(), 100000);
    }
}
