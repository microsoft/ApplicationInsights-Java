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

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class HttpRequestTelemetryTest {

    @Test
    public void testDefaultCtor() {
        HttpRequestTelemetry requestTelemetry = new HttpRequestTelemetry();

        assertNotNull(requestTelemetry.getTimestamp());
        assertEquals(requestTelemetry.getResponseCode(), "200");
        assertEquals(requestTelemetry.isSuccess(), true);
    }

    @Test
    public void testParameterizedCtor() {
        Date date = new Date();
        HttpRequestTelemetry requestTelemetry = new HttpRequestTelemetry("mockName", date, 1010, "200", true);

        assertEquals(requestTelemetry.getName(), "mockName");
        assertEquals(requestTelemetry.getTimestamp(), date);
        assertEquals(requestTelemetry.getDuration(), 1010);
        assertEquals(requestTelemetry.getResponseCode(), "200");
        assertEquals(requestTelemetry.isSuccess(), true);
    }

    @Test
    public void testSetCode() {
        Date date = new Date();
        HttpRequestTelemetry requestTelemetry = new HttpRequestTelemetry("mockName", date, 1010, "200", true);
        requestTelemetry.setResponseCode("400");

        assertEquals(requestTelemetry.getResponseCode(), "400");
        assertEquals(requestTelemetry.isSuccess(), true);
    }
}