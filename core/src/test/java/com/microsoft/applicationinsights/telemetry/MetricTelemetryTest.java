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

import com.microsoft.applicationinsights.internal.util.Sanitizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class MetricTelemetryTest {
    @Test
    public void testEmptyCtor() {
        MetricTelemetry telemetry = new MetricTelemetry();

        assertNull(telemetry.getName());
        assertNull(telemetry.getValue());
        assertNull(telemetry.getCount());
        assertNull(telemetry.getMin());
        assertNull(telemetry.getMax());
        assertEquals(telemetry.getStandardDeviation(), null);
    }


    @Test
    public void testCtor() {
        MetricTelemetry telemetry = new MetricTelemetry("MockName", 120.1);

        assertEquals(telemetry.getName(), "MockName");
        assertEquals(telemetry.getValue(), 120.1, 0);
        assertEquals(telemetry.getCount(), null);
        assertNull(telemetry.getCount());
        assertNull(telemetry.getMin());
        assertNull(telemetry.getMax());
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