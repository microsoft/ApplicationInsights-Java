/*
 * AppInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.telemetry;

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
