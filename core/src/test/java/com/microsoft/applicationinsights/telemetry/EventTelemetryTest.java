package com.microsoft.applicationinsights.telemetry;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public final class EventTelemetryTest {
    @Test
    public void testDefaultCtor() {
        EventTelemetry eventTelemetry = new EventTelemetry();
        String name = eventTelemetry.getName();
        assertEquals(name, null);
    }

    @Test
    public void testSetName() {
        EventTelemetry eventTelemetry = new EventTelemetry("mockname");
        assertEquals(eventTelemetry.getName(), "mockname");

        eventTelemetry.setName("new name");
        assertEquals(eventTelemetry.getName(), "new name");
    }

    @Test
    public void testSetTimestamp() {
        EventTelemetry eventTelemetry = new EventTelemetry("mockname");

        Date date = new Date();
        eventTelemetry.setTimestamp(date);
        assertEquals(eventTelemetry.getTimestamp(), date);
    }

    public void testSanitize() {
    }
}
