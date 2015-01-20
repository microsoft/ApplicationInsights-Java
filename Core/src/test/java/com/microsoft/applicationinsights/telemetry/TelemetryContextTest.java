package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.extensibility.context.ContextTagKeys;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public final class TelemetryContextTest {
    @Test
    public void testCtor() {
        TelemetryContext context = new TelemetryContext();

        assertTrue(context.getProperties().isEmpty());
        assertTrue(context.getTags().isEmpty());
        assertNull(context.getInstrumentationKey());
    }

    @Test
    public void testSetInstrumentationKey() {
        TelemetryContext context = new TelemetryContext();
        context.setInstrumentationKey("key");

        assertEquals("key", context.getInstrumentationKey());
    }
}
