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

package com.microsoft.applicationinsights.web.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * Created by yonisha on 5/27/2015.
 */
@Ignore
public class ApplicationInsightsHttpResponseWrapperTests {

    private HttpServletResponse responseMock = mock(HttpServletResponse.class);
    private ApplicationInsightsHttpResponseWrapper wrapperUnderTest;

    @Before
    public void testInitialize() {
        wrapperUnderTest = new ApplicationInsightsHttpResponseWrapper(responseMock);
    }

    @Test
    public void testDefaultStatusCode() {
        verifyStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void testSetStatus() {
        wrapperUnderTest.setStatus(HttpServletResponse.SC_BAD_GATEWAY);

        verifyStatus(HttpServletResponse.SC_BAD_GATEWAY);
    }

    @Test
    public void testSendError() throws IOException {
        wrapperUnderTest.sendError(HttpServletResponse.SC_BAD_REQUEST);

        verifyStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testSendErrorWithMessage() throws IOException {
        final String errorMessage = "FATAL!";
        wrapperUnderTest.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage);

        verifyStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testSendRedirect() throws IOException {
        wrapperUnderTest.sendRedirect("some location");

        verifyStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    @Test
    public void testReset() throws IOException {
        wrapperUnderTest.setStatus(HttpServletResponse.SC_CONFLICT);
        wrapperUnderTest.reset();

        verifyStatus(HttpServletResponse.SC_OK);
    }

    private void verifyStatus(int expectedStatus) {
        Assert.assertEquals(expectedStatus, wrapperUnderTest.getStatus());
    }
}
