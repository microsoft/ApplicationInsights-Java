package com.microsoft.applicationinsights.telemetry;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public final class HttpRequestTelemetryTest {
    @Test
    public void testCtor() {
        Date date = new Date();
        HttpRequestTelemetry requestTelemetry = new HttpRequestTelemetry("mockName", date, 1010, "200", true);

        assertEquals(requestTelemetry.getName(), "mockName");
        assertEquals(requestTelemetry.getTimestamp(), date);
        assertEquals(requestTelemetry.getDuration(), 1010);
        assertEquals(requestTelemetry.getResponseCode(), "200");
        assertEquals(requestTelemetry.isSuccess(), true);
    }

    @Test
    public void testSetCode() {
        Date date = new Date();
        HttpRequestTelemetry requestTelemetry = new HttpRequestTelemetry("mockName", date, 1010, "200", true);
        requestTelemetry.setResponseCode("400");

        assertEquals(requestTelemetry.getResponseCode(), "400");
        assertEquals(requestTelemetry.isSuccess(), true);
    }
}