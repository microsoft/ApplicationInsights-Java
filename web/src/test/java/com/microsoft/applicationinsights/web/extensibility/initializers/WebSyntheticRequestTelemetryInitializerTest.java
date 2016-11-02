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

import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;

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

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.doReturn("").when(mockRequest).getHeader(anyString());

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime(), mockRequest);
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

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.doReturn("").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_SOURCE);
        Mockito.doReturn("A1").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_RUN_ID);
        Mockito.doReturn("A2").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_LOCATION);

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime(), mockRequest);
        ThreadContext.setRequestTelemetryContext(context);

        TraceTelemetry telemetry = new TraceTelemetry();
        tested.initialize(telemetry);

        assertEquals(telemetry.getContext().getOperation().getSyntheticSource(), WebSyntheticRequestTelemetryInitializer.SYNTHETIC_SOURCE_NAME);
        assertEquals(telemetry.getContext().getSession().getId(), "A1");
        assertEquals(telemetry.getContext().getUser().getId(), "A2");
    }

    @Test
    public void commonSyntheticHeadersTest() {

        WebSyntheticRequestTelemetryInitializer tested = new WebSyntheticRequestTelemetryInitializer();

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.doReturn("A").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_SOURCE);
        Mockito.doReturn("A1").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_USER_ID);
        Mockito.doReturn("A2").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_SESSION_ID);
        Mockito.doReturn("A3").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_OPERATION_ID);
        Mockito.doReturn("A4").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_TEST_NAME);
        Mockito.doReturn("A5").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_RUN_ID);
        Mockito.doReturn("A6").when(mockRequest).getHeader(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_LOCATION);

        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime(), mockRequest);
        ThreadContext.setRequestTelemetryContext(context);

        TraceTelemetry telemetry = new TraceTelemetry();
        tested.initialize(telemetry);

        assertEquals(telemetry.getContext().getOperation().getSyntheticSource(), "A");
        assertEquals(telemetry.getContext().getUser().getId(), "A1");
        assertEquals(telemetry.getContext().getSession().getId(), "A2");
        assertEquals(telemetry.getContext().getOperation().getId(), "A3");
        assertEquals(telemetry.getContext().getProperties().get(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_TEST_NAME), "A4");
        assertEquals(telemetry.getContext().getProperties().get(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_RUN_ID), "A5");
        assertEquals(telemetry.getContext().getProperties().get(WebSyntheticRequestTelemetryInitializer.SYNTHETIC_TEST_LOCATION), "A6");
    }
}