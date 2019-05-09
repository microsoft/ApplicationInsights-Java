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

package com.microsoft.applicationinsights.web.extensibility.initializers;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class WebSyntheticRequestTelemetryInitializerTest {
    @Test
    public void noRequestTest() {

        WebSyntheticRequestTelemetryInitializer tested = new WebSyntheticRequestTelemetryInitializer();
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        Telemetry mockTelemetry = Mockito.mock(Telemetry.class);
        tested.initialize(mockTelemetry);

        Mockito.verify(mockTelemetry, Mockito.never()).getContext();
    }

    @Test
    public void noSyntheticHeadersTest() {

        WebSyntheticRequestTelemetryInitializer tested = new WebSyntheticRequestTelemetryInitializer();

        Map<String, String> requestHeaders = new HashMap<>();

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime(), requestHeaders);
        ThreadContext.setRequestTelemetryContext(context);

        TraceTelemetry telemetry = new TraceTelemetry();
        tested.initialize(telemetry);

        assertTrue(CommonUtils.isNullOrEmpty(telemetry.getContext().getSession().getId()));
        assertTrue(CommonUtils.isNullOrEmpty(telemetry.getContext().getUser().getId()));
        assertTrue(CommonUtils.isNullOrEmpty(telemetry.getContext().getOperation().getId()));
    }

    @Test
    public void gsmSyntheticHeadersTest() {

        WebSyntheticRequestTelemetryInitializer tested = new WebSyntheticRequestTelemetryInitializer();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_SOURCE, "");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_RUN_ID, "A1");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_LOCATION, "A2");

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime(), requestHeaders);
        ThreadContext.setRequestTelemetryContext(context);

        TraceTelemetry telemetry = new TraceTelemetry();
        tested.initialize(telemetry);

        assertEquals(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_SOURCE_NAME, telemetry.getContext().getOperation().getSyntheticSource());
        assertEquals("A1", telemetry.getContext().getSession().getId());
        assertEquals("A2", telemetry.getContext().getUser().getId());
    }

    @Test
    public void commonSyntheticHeadersTest() {

        WebSyntheticRequestTelemetryInitializer tested = new WebSyntheticRequestTelemetryInitializer();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_SOURCE, "A");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_USER_ID, "A1");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_SESSION_ID, "A2");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_OPERATION_ID, "A3");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_TEST_NAME, "A4");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_RUN_ID, "A5");
        requestHeaders.put(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_LOCATION, "A6");

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime(), requestHeaders);
        ThreadContext.setRequestTelemetryContext(context);

        TraceTelemetry telemetry = new TraceTelemetry();
        tested.initialize(telemetry);

        assertEquals("A", telemetry.getContext().getOperation().getSyntheticSource());
        assertEquals("A1", telemetry.getContext().getUser().getId());
        assertEquals("A2", telemetry.getContext().getSession().getId());
        assertEquals("A3", telemetry.getContext().getOperation().getId());
        assertEquals("A4", telemetry.getContext().getProperties().get(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_TEST_NAME));
        assertEquals("A5", telemetry.getContext().getProperties().get(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_RUN_ID));
        assertEquals("A6", telemetry.getContext().getProperties().get(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_LOCATION));
    }
}