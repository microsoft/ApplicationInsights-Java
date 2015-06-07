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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;
import com.microsoft.applicationinsights.internal.reflect.ClassDataVerifier;
import org.junit.Assert;
import org.junit.Test;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.microsoft.applicationinsights.web.utils.ServletUtils;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Created by yonisha on 2/3/2015.
 */
public class WebRequestTrackingFilterTests {

    private class StubTelemetryClient extends TelemetryClient {
        public int trackExceptionCalled;

        public boolean shouldThrow;

        @Override
        public void trackException(Exception exception) {
            ++trackExceptionCalled;
            if (shouldThrow) {
                throw new RuntimeException();
            }
        }
    }

    private static class FilterAndTelemetryClientMock {
        public final Filter filter;
        public final StubTelemetryClient mockTelemetryClient;

        private FilterAndTelemetryClientMock(Filter filter, StubTelemetryClient mockTelemetryClient) {
            this.filter = filter;
            this.mockTelemetryClient = mockTelemetryClient;
        }
    }

    @Test
    public void testFilterInitializedSuccessfullyFromConfiguration() throws ServletException {
        Filter filter = createInitializedFilter();
        WebModulesContainer container = ServletUtils.getWebModuleContainer(filter);

        Assert.assertNotNull("Container shouldn't be null", container);
        Assert.assertTrue("Modules container shouldn't be empty", container.getModulesCount() > 0);
    }

    @Test
    public void testFiltersChainWhenExceptionIsThrownOnModulesInvocation() throws Exception {
        Filter filter = createInitializedFilter();

        // mocking
        WebModulesContainer containerMock = ServletUtils.setMockWebModulesContainer(filter);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception("FATAL!");
            }
        }).when(containerMock).invokeOnBeginRequest(any(ServletRequest.class), any(ServletResponse.class));

        FilterChain chain = mock(FilterChain.class);

        ServletRequest request = ServletUtils.generateDummyServletRequest();

        // execute
        filter.doFilter(request, ServletUtils.generateDummyServletResponse(), chain);

        // validate
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    public void testUnhandledRuntimeExceptionWithTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClient();
        testException(createdData, new java.lang.IllegalArgumentException());
    }

    @Test
    public void testUnhandledRuntimeExceptionWithoutTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithoutTelemetryClient();
        testException(createdData, new RuntimeException());
    }

    @Test
    public void testUnhandledRuntimeExceptionWithTelemetryClientThatThrows() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClientThatThrows();
        testException(createdData, new RuntimeException());
    }

    @Test
    public void testUnhandledServletExceptionWithTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClient();
        testException(createdData, new ServletException());
    }

    @Test
    public void testUnhandledServletExceptionWithoutTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithoutTelemetryClient();
        testException(createdData, new ServletException());
    }

    @Test
    public void testUnhandledServletExceptionWithTelemetryClientThatThrows() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClientThatThrows();
        testException(createdData, new ServletException());
    }

    @Test
    public void testUnhandledIOExceptionWithTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClient();
        testException(createdData, new IOException());
    }

    @Test
    public void testUnhandledIOExceptionWithoutTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithoutTelemetryClient();
        testException(createdData, new IOException());
    }

    @Test
    public void testUnhandledIOExceptionWithTelemetryClientThatThrows() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClientThatThrows();
        testException(createdData, new IOException());
    }

    // region Private methods

    private void testException(FilterAndTelemetryClientMock createdData, Exception expectedException) throws NoSuchFieldException, IllegalAccessException, ServletException {

        try {
            FilterChain chain = mock(FilterChain.class);

            ServletRequest request = ServletUtils.generateDummyServletRequest();
            ServletResponse response = ServletUtils.generateDummyServletResponse();
            Mockito.doThrow(expectedException).when(chain).doFilter(eq(request), any(ServletResponse.class));

            // execute
            createdData.filter.doFilter(request, response, chain);

            assertFalse("doFilter should have throw", true);
        } catch (Exception se) {
            Assert.assertSame(expectedException, se);

            if (createdData.mockTelemetryClient != null) {
                Assert.assertTrue(createdData.mockTelemetryClient.trackExceptionCalled == 1);
            }
        }
    }

    private Filter createInitializedFilter() throws ServletException {
        Filter filter = new WebRequestTrackingFilter();
        filter.init(null);

        return filter;
    }

    private FilterAndTelemetryClientMock createInitializedFilterWithTelemetryClientThatThrows() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedFilterWithMockTelemetryClient(true, true);
    }

    private FilterAndTelemetryClientMock createInitializedFilterWithTelemetryClient() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedFilterWithMockTelemetryClient(true, false);
    }

    private FilterAndTelemetryClientMock createInitializedFilterWithoutTelemetryClient() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedFilterWithMockTelemetryClient(false, false);
    }

    private FilterAndTelemetryClientMock createInitializedFilterWithMockTelemetryClient(boolean withTelemetryClient, boolean clientThrows) throws ServletException, NoSuchFieldException, IllegalAccessException {
        Filter filter = createInitializedFilter();


        Field field = WebRequestTrackingFilter.class.getDeclaredField("telemetryClient");
        field.setAccessible(true);

        StubTelemetryClient mockTelemetryClient = null;
        if (withTelemetryClient) {
            mockTelemetryClient = new StubTelemetryClient();
            if (clientThrows) {
                mockTelemetryClient.shouldThrow = true;
            }
        }
        field.set(filter, mockTelemetryClient);

        return new FilterAndTelemetryClientMock(filter, mockTelemetryClient);
    }
    // endregion Private methods
}