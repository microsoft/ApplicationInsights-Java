package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static org.junit.Assert.assertEquals;

public class NetworkStatsbeatTest {

    private NetworkStatsbeat networkStatsbeat;

    @Before
    public void init() {
        StatsbeatModule.getInstance().initialize(new TelemetryClient());
        networkStatsbeat = StatsbeatModule.getInstance().getNetworkStatsbeat();
    }

    @Test
    public void testAddInstrumentation() {
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.jdbc");
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.tomcat-7.0");
        networkStatsbeat.addInstrumentation("io.opentelemetry.javaagent.http-url-connection");
        assertEquals(networkStatsbeat.getInstrumentation(), 144115188077961216L);
    }

    @Test
    public void testIncrementRequestSuccessCount() {
        assertEquals(networkStatsbeat.getRequestSuccessCount(), 0);
        networkStatsbeat.incrementRequestSuccessCount();
        networkStatsbeat.incrementRequestSuccessCount();
        assertEquals(networkStatsbeat.getRequestSuccessCount(), 2);
        networkStatsbeat.reset();
        assertEquals(networkStatsbeat.getRequestSuccessCount(), 0);
    }

    @Test
    public void testIncrementRequestFailureCount() {
        assertEquals(networkStatsbeat.getRequestFailureCount(), 0);
        networkStatsbeat.incrementRequestFailureCount();
        networkStatsbeat.incrementRequestFailureCount();
        assertEquals(networkStatsbeat.getRequestFailureCount(), 2);
        networkStatsbeat.reset();
        assertEquals(networkStatsbeat.getRequestFailureCount(), 0);
    }

    @Test
    public void testAddRequestDuration() {
        assertEquals(networkStatsbeat.getRequestDurations().size(), 0);
        networkStatsbeat.addRequestDuration(1000);
        networkStatsbeat.addRequestDuration(3000);
        assertEquals(networkStatsbeat.getRequestDurations().size(), 2);
        assertEquals(networkStatsbeat.getRequestDurationAvg(), 2000.0, 0);
        networkStatsbeat.reset();
        assertEquals(networkStatsbeat.getRequestDurations().size(), 0);
        assertEquals(networkStatsbeat.getRequestDurationAvg(), 0.0, 0);
    }

    @Test
    public void testIncrementRetryCount() {
        assertEquals(networkStatsbeat.getRetryCount(), 0);
        networkStatsbeat.incrementRetryCount();
        networkStatsbeat.incrementRetryCount();
        assertEquals(networkStatsbeat.getRetryCount(), 2);
        networkStatsbeat.reset();
        assertEquals(networkStatsbeat.getRetryCount(), 0);
    }

    @Test
    public void testIncrementThrottlingCount() {
        assertEquals(networkStatsbeat.getThrottlingCount(), 0);
        networkStatsbeat.incrementThrottlingCount();
        networkStatsbeat.incrementThrottlingCount();
        assertEquals(networkStatsbeat.getThrottlingCount(), 2);
        networkStatsbeat.reset();
        assertEquals(networkStatsbeat.getThrottlingCount(), 0);
    }

    @Test
    public void testIncrementExceptionCount() {
        assertEquals(networkStatsbeat.getExceptionCount(), 0);
        networkStatsbeat.incrementExceptionCount();
        networkStatsbeat.incrementExceptionCount();
        assertEquals(networkStatsbeat.getExceptionCount(), 2);
        networkStatsbeat.reset();
        assertEquals(networkStatsbeat.getExceptionCount(), 0);
    }

    @Test
    public void testInterval() {
        assertEquals(networkStatsbeat.getInterval(), DEFAULT_STATSBEAT_INTERVAL);
    }
}
