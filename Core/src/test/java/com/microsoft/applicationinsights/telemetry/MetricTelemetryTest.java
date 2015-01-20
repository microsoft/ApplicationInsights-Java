package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.util.Sanitizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MetricTelemetryTest {
    @Test
    public void testEmptyCtor() {
        MetricTelemetry telemetry = new MetricTelemetry();

        assertEquals(telemetry.getName(), null);
        assertEquals(telemetry.getValue(), 0.0, 0);
        assertEquals(telemetry.getCount(), null);
        assertEquals(telemetry.getMin(), null);
        assertEquals(telemetry.getMax(), null);
        assertEquals(telemetry.getStandardDeviation(), null);
    }


    @Test
    public void testCtor() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);

        assertEquals(telemetry.getName(), "MockName");
        assertEquals(telemetry.getValue(), 120.1, 0);
        assertEquals(telemetry.getCount(), null);
        assertEquals(telemetry.getCount(), null);
        assertEquals(telemetry.getMin(), null);
        assertEquals(telemetry.getMax(), null);
        assertEquals(telemetry.getStandardDeviation(), null);
    }

    @Test
    public void testSetName() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);
        telemetry.setName("MockName1");

        assertEquals(telemetry.getName(), "MockName1");
        assertEquals(telemetry.getValue(), 120.1, 0);
    }

    @Test
    public void testSetValue() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);
        telemetry.setValue(240.0);

        assertEquals(telemetry.getName(), "MockName");
        assertEquals(telemetry.getValue(), 240.0, 0);
    }

    @Test
    public void testSetCount() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);
        telemetry.setCount(1);

        assertEquals(telemetry.getCount(), new Integer(1));
    }

    @Test
    public void testSetMin() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);
        telemetry.setMin(new Double(1));

        assertEquals(telemetry.getMin(), new Double(1));
    }

    @Test
    public void testSetMax() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);
        telemetry.setMax(new Double(1));

        assertEquals(telemetry.getMax(), new Double(1));
    }

    @Test
    public void testSetStandardDeviation() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);
        telemetry.setStandardDeviation(new Double(1));

        assertEquals(telemetry.getStandardDeviation(), new Double(1));
    }

    @Test
    public void testSanitizeLongName() throws Exception {
        MetricTelemetry telemetry = new MetricTelemetry(TelemetryTestsUtils.createString(Sanitizer.MAX_NAME_LENGTH), 120.1);

        telemetry.sanitize();
        assertEquals(telemetry.getName().length(), Sanitizer.MAX_NAME_LENGTH);
    }
}