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

package com.microsoft.applicationinsights.telemetry;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PageViewTelemetryTest {
    private final static String NON_VALID_URL = "http:sd{@~fsd.s.d.f;fffff";

    @Test
    public void testEmptyCtor() {
        PageViewTelemetry telemetry = new PageViewTelemetry();

        assertNull(telemetry.getName());
        assertNull(telemetry.getUri());
        assertTrue(telemetry.getMetrics().isEmpty());
        assertTrue(telemetry.getProperties().isEmpty());
        assertEquals(telemetry.getDuration(), 0);
    }

    @Test
    public void testCtor() {
        PageViewTelemetry telemetry = new PageViewTelemetry("MockName");

        assertEquals(telemetry.getName(), "MockName");
        assertNull(telemetry.getUri());
        assertTrue(telemetry.getMetrics().isEmpty());
        assertTrue(telemetry.getProperties().isEmpty());
        assertEquals(telemetry.getDuration(), 0);
    }

    @Test
    public void testSetName() {
        PageViewTelemetry telemetry = new PageViewTelemetry("MockName");

        telemetry.setName("MockName1");
        assertEquals(telemetry.getName(), "MockName1");
    }

    @Test
    public void testSetDuration() {
        PageViewTelemetry telemetry = new PageViewTelemetry("MockName");

        telemetry.setDuration(2001);
        assertEquals(telemetry.getDuration(), 2001);
    }

    @Test
    public void testSetUri() throws URISyntaxException {
        PageViewTelemetry telemetry = new PageViewTelemetry();

        URI uri = new URI("http://microsoft.com/");
        telemetry.setUrl(uri);
        assertEquals(telemetry.getUri(), uri);
    }
}
