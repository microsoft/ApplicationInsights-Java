/*
 * ApplicationInsights-Java
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

package com.microsoft.applicationinsights.internal.processor;

import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by gupele on 7/26/2016.
 */
public class TraceTelemetryFilterTest {
    @Test(expected = Throwable.class)
    public void testProcessWithNullData() throws Throwable {
        TraceTelemetryFilter tested = new TraceTelemetryFilter();
        tested.setFromTraceLevel(null);
        TraceTelemetry traceTelemetry = new TraceTelemetry();
        traceTelemetry.setMessage("A A 1");
        boolean result = tested.process(traceTelemetry);

        assertTrue(result);
    }

    @Test(expected = Throwable.class)
    public void testProcessWithEmptyData() throws Throwable {
        TraceTelemetryFilter tested = new TraceTelemetryFilter();
        tested.setFromTraceLevel("");
        TraceTelemetry traceTelemetry = new TraceTelemetry();
        traceTelemetry.setMessage("A A 1");
        boolean result = tested.process(traceTelemetry);

        assertTrue(result);
    }

    @Test
    public void testProcessOffSeverityLevel() throws Throwable {
        TraceTelemetryFilter tested = new TraceTelemetryFilter();
        tested.setFromTraceLevel(" off");

        for (SeverityLevel sl : SeverityLevel.values()) {
            TraceTelemetry traceTelemetry = new TraceTelemetry();
            traceTelemetry.setSeverityLevel(sl);
            traceTelemetry.setMessage("A A 1");
            boolean result = tested.process(traceTelemetry);

            assertFalse(result);
        }
    }

    @Test
    public void testProcessWarningLevel() throws Throwable {
        TraceTelemetryFilter tested = new TraceTelemetryFilter();
        tested.setFromTraceLevel(" Warn");

        for (SeverityLevel sl : SeverityLevel.values()) {
            TraceTelemetry traceTelemetry = new TraceTelemetry();
            traceTelemetry.setSeverityLevel(sl);
            traceTelemetry.setMessage("A A 1");
            boolean result = tested.process(traceTelemetry);

            if (sl.equals(SeverityLevel.Verbose) || sl.equals(SeverityLevel.Information)) {
                assertFalse(result);
            } else {
                assertTrue(result);
            }
        }
    }

    @Test
    public void testProcessWithMetricTelemetry() throws Throwable {
        TraceTelemetryFilter tested = new TraceTelemetryFilter();
        tested.setFromTraceLevel(" trace ");
        boolean result = tested.process(new MetricTelemetry());
        assertTrue(result);
    }
}
