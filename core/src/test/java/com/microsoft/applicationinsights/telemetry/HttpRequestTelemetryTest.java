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

import java.util.Date;
import org.junit.Test;
import org.apache.http.HttpStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class HttpRequestTelemetryTest {

    @Test
    public void testDefaultCtor() {
        RequestTelemetry requestTelemetry = new RequestTelemetry();

        assertNotNull(requestTelemetry.getTimestamp());
        assertEquals(Integer.toString(HttpStatus.SC_OK), requestTelemetry.getResponseCode());
        assertTrue(requestTelemetry.isSuccess());
    }

    @Test
    public void testParameterizedCtor() {
        Date date = new Date();
        RequestTelemetry requestTelemetry = new RequestTelemetry("mockName", date, 1010, "200", true);

        assertEquals("mockName", requestTelemetry.getName());
        assertEquals(date, requestTelemetry.getTimestamp());
        assertEquals("00:00:01.0100000", requestTelemetry.getDuration().toString());
        assertEquals("200", requestTelemetry.getResponseCode());
        assertTrue(requestTelemetry.isSuccess());
    }

    @Test
    public void testSetCode() {
        Date date = new Date();
        RequestTelemetry requestTelemetry = new RequestTelemetry("mockName", date, 1010, "200", true);
        requestTelemetry.setResponseCode("400");

        assertEquals("400", requestTelemetry.getResponseCode());
        // TODO FIXME ummm....400 is success? or should the test be named setCodeDoesNotUpdateSuccessState?
        assertTrue(requestTelemetry.isSuccess());
    }
}