package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.DependencyKind;
import com.microsoft.applicationinsights.internal.schemav2.DependencySourceType;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class RemoteDependencyTelemetryTest {
    @Test
    public void testEmptyCtor() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();

        assertNull(telemetry.getName());
        assertEquals(telemetry.getValue(), 0.0, 0);
        assertNull(telemetry.getCount());
        assertNull(telemetry.getMin());
        assertNull(telemetry.getMax());
        assertNull(telemetry.getStdDev());
        assertEquals(telemetry.getDependencyKind(), DependencyKind.Undefined);
        assertEquals(telemetry.getDependencySource(), DependencySourceType.Undefined);
        assertTrue(telemetry.getProperties().isEmpty());
    }

    @Test
    public void testCtor() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        assertEquals(telemetry.getName(), "MockName");
        assertEquals(telemetry.getValue(), 0.0, 0);
        assertNull(telemetry.getCount());
        assertNull(telemetry.getMin());
        assertNull(telemetry.getMax());
        assertNull(telemetry.getStdDev());
        assertEquals(telemetry.getDependencyKind(), DependencyKind.Undefined);
        assertEquals(telemetry.getDependencySource(), DependencySourceType.Undefined);
        assertTrue(telemetry.getProperties().isEmpty());
    }

    @Test
    public void testSetName() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setName("MockName1");
        assertEquals(telemetry.getName(), "MockName1");
    }

    @Test
    public void testSetValue() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setValue(120.1);
        assertEquals(telemetry.getValue(), 120.1, 0);
    }

    @Test
    public void testSetCount() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setCount(new Integer(1));
        assertEquals(telemetry.getCount(), new Integer(1));
    }

    @Test
    public void testSetMin() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setMin(new Double(1));
        assertEquals(telemetry.getMin(), new Double(1));
    }

    @Test
    public void testSetMax() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setMax(new Double(1));
        assertEquals(telemetry.getMax(), new Double(1));
    }

    @Test
    public void testSetStdDev() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setStdDev(new Double(1));
        assertEquals(telemetry.getStdDev(), new Double(1));
    }

    @Test
    public void testDependencyKind() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setDependencyKind(DependencyKind.HttpAny);
        assertEquals(telemetry.getDependencyKind(), DependencyKind.HttpAny);
    }

    @Test
    public void testSetDependencySource() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setDependencySource(DependencySourceType.Aic);
        assertEquals(telemetry.getDependencySource(), DependencySourceType.Aic);
    }
}
