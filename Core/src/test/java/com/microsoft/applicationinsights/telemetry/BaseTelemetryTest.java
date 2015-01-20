package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.SendableData;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class BaseTelemetryTest {
    private static class StubSendableData implements SendableData {
        @Override
        public String getEnvelopName() {
            return null;
        }

        @Override
        public String getBaseTypeName() {
            return null;
        }

        @Override
        public void serialize(JsonTelemetryDataSerializer serializer) throws IOException {

        }
    }

    private static class StubTelemetry extends BaseTelemetry<StubSendableData> {
        public int numberOfCallsToAdditionalSanitize;

        public StubTelemetry() {
        }

        public StubTelemetry(String d) {
            initialize(new ConcurrentHashMap<String, String>());
        }

        @Override
        protected void additionalSanitize() {
            ++numberOfCallsToAdditionalSanitize;
        }

        @Override
        protected StubSendableData getData() {
            return null;
        }
    }

    @Test
    public void testCtor() {
        StubTelemetry telemetry = new StubTelemetry();

        assertNull(telemetry.getContext());
        assertNull(telemetry.getTimestamp());
    }

    @Test
    public void testCtorWithInitialize() {
        StubTelemetry telemetry = new StubTelemetry("1");

        assertNotNull(telemetry.getContext());
        assertTrue(telemetry.getContext().getProperties().isEmpty());
        assertTrue(telemetry.getContext().getTags().isEmpty());
        assertTrue(telemetry.getProperties().isEmpty());
        assertNull(telemetry.getTimestamp());
    }

    @Test
    public void testSetTimestamp() {
        StubTelemetry telemetry = new StubTelemetry();

        Date date = new Date();
        telemetry.setTimestamp(date);

        assertEquals(telemetry.getTimestamp(), date);
    }

    @Test
    public void testSanitize() {
        StubTelemetry telemetry = new StubTelemetry("1");

        telemetry.sanitize();

        assertEquals(telemetry.numberOfCallsToAdditionalSanitize, 1);
    }
}