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

import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by gupele on 7/26/2016.
 */
public class SyntheticSourceFilterTest {

    @Test
    public void testNullSources() {
        SyntheticSourceFilter tested = new SyntheticSourceFilter();
        boolean result = tested.process(new PageViewTelemetry());

        assertTrue(result);
    }

    @Test
    public void testEmptySources() throws Throwable {
        SyntheticSourceFilter tested = new SyntheticSourceFilter();
        tested.setNotNeededSources("");
        boolean result = tested.process(new PageViewTelemetry());

        assertTrue(result);
    }

    @Test
    public void testNullTelemetry() throws Throwable {
        SyntheticSourceFilter tested = new SyntheticSourceFilter();
        tested.setNotNeededSources("a");
        boolean result = tested.process(null);

        assertTrue(result);
    }


    @Test
    public void testOneSourceThatIsFound() throws Throwable {
        SyntheticSourceFilter tested = new SyntheticSourceFilter();
        tested.setNotNeededSources("A");
        Telemetry telemetry = new PageViewTelemetry();
        telemetry.getContext().getOperation().setSyntheticSource("A");
        boolean result = tested.process(telemetry);

        assertFalse(result);
    }

    @Test
    public void testOneSourceThatIsNotFound() throws Throwable {
        SyntheticSourceFilter tested = new SyntheticSourceFilter();
        tested.setNotNeededSources("A");
        Telemetry telemetry = new PageViewTelemetry();
        telemetry.getContext().getOperation().setSyntheticSource("B");
        boolean result = tested.process(telemetry);

        assertTrue(result);
    }

    @Test
    public void testMultipleSourcesThatIsNotFound() throws Throwable {
        SyntheticSourceFilter tested = new SyntheticSourceFilter();
        tested.setNotNeededSources("F, B,C,D, E , A,");

        String[] unneededSources = {"A", "B", "C", "D", "E", "F"};
        String[] neededSources = {"A1", "H"};

        for (String unneeded : unneededSources){
            Telemetry telemetry = new PageViewTelemetry();
            telemetry.getContext().getOperation().setSyntheticSource(unneeded);
            boolean result = tested.process(telemetry);
            assertFalse(result);
        }

        for (String needed : neededSources){
            Telemetry telemetry = new PageViewTelemetry();
            telemetry.getContext().getOperation().setSyntheticSource(needed);
            boolean result = tested.process(telemetry);
            assertTrue(result);
        }
    }
}
