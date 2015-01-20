package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.util.Sanitizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class TraceTelemetryTest {
    @Test
    public void testEmptyCtor() {
        TraceTelemetry telemetry = new TraceTelemetry();

        assertNull(telemetry.getMessage());
    }

    @Test
    public void testCtor() {
        TraceTelemetry telemetry = new TraceTelemetry("MockMessage");

        assertEquals(telemetry.getMessage(), "MockMessage");
    }

    @Test
    public void testSetMessage() {
        TraceTelemetry telemetry = new TraceTelemetry("MockMessage");

        telemetry.setMessage("MockMessage1");
        assertEquals(telemetry.getMessage(), "MockMessage1");
    }

    @Test
    public void testSanitize() {
        TraceTelemetry telemetry = new TraceTelemetry(TelemetryTestsUtils.createString(Sanitizer.MAX_NAME_LENGTH));

        telemetry.sanitize();
        assertEquals(telemetry.getMessage().length(), Sanitizer.MAX_NAME_LENGTH);
    }
}