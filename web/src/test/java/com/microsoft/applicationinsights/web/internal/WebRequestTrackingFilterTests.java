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
import com.microsoft.applicationinsights.web.utils.ServletUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    /* For parametrized constructor initialization test */
    @Test
    public void testParametrizedFilterInitializedSuccessfullyFromConfiguration() throws ServletException {
        Filter filter = createParametrizedInitializedFilter("My-App");
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


    /* For Parametrized Name setting */
    @Test
    public void testParametrizedFiltersChainWhenExceptionIsThrownOnModulesInvocation() throws Exception {
        Filter filter = createParametrizedInitializedFilter("My-App");

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

        //For parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithTelemetryClient();
        testException(createdDataParametrized, new java.lang.IllegalArgumentException());
    }


    @Test
    public void testUnhandledRuntimeExceptionWithoutTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithoutTelemetryClient();
        testException(createdData, new RuntimeException());

        //For Parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithoutTelemetryClient();
        testException(createdDataParametrized, new RuntimeException());

    }

    @Test
    public void testUnhandledRuntimeExceptionWithTelemetryClientThatThrows() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClientThatThrows();
        testException(createdData, new RuntimeException());

        //For Parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithTelemetryClientThatThrows();
        testException(createdDataParametrized, new RuntimeException());
    }

    @Test
    public void testUnhandledServletExceptionWithTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClient();
        testException(createdData, new ServletException());

        //For parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithTelemetryClient();
        testException(createdDataParametrized, new ServletException());
    }

    @Test
    public void testUnhandledServletExceptionWithoutTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithoutTelemetryClient();
        testException(createdData, new ServletException());

        //For Parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithoutTelemetryClient();
        testException(createdDataParametrized, new ServletException());
    }

    @Test
    public void testUnhandledServletExceptionWithTelemetryClientThatThrows() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClientThatThrows();
        testException(createdData, new ServletException());

        //For Parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithTelemetryClientThatThrows();
        testException(createdDataParametrized, new ServletException());

    }

    @Test
    public void testUnhandledIOExceptionWithTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClient();
        testException(createdData, new IOException());

        //For parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithTelemetryClient();
        testException(createdDataParametrized, new IOException());
    }

    @Test
    public void testUnhandledIOExceptionWithoutTelemetryClient() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithoutTelemetryClient();
        testException(createdData, new IOException());

        //For Parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithoutTelemetryClient();
        testException(createdDataParametrized, new IOException());
    }

    @Test
    public void testUnhandledIOExceptionWithTelemetryClientThatThrows() throws IllegalAccessException, NoSuchFieldException, ServletException {
        FilterAndTelemetryClientMock createdData = createInitializedFilterWithTelemetryClientThatThrows();
        testException(createdData, new IOException());

        //For Parametrized
        FilterAndTelemetryClientMock createdDataParametrized = createParametrizedInitializedFilterWithTelemetryClientThatThrows();
        testException(createdDataParametrized, new IOException());
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

    private Filter createParametrizedInitializedFilter(String appName) throws ServletException {
        Filter filter = new WebRequestTrackingFilter(appName);

        filter.init(null);
        return filter;
    }

    private FilterAndTelemetryClientMock createInitializedFilterWithTelemetryClientThatThrows() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedFilterWithMockTelemetryClient(true, true);
    }

    //For Parametrized name initialization
    private FilterAndTelemetryClientMock createParametrizedInitializedFilterWithTelemetryClientThatThrows() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedFilterWithMockTelemetryClient(true, true);
    }

    private FilterAndTelemetryClientMock createInitializedFilterWithTelemetryClient() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedFilterWithMockTelemetryClient(true, false);
    }

    //For parametrized name initialization
    private FilterAndTelemetryClientMock createParametrizedInitializedFilterWithTelemetryClient() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedParametrizedFilterWithMockTelemetryClient(true, false);
    }


    private FilterAndTelemetryClientMock createInitializedFilterWithoutTelemetryClient() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedFilterWithMockTelemetryClient(false, false);
    }

    //For parametrized name initialization
    private FilterAndTelemetryClientMock createParametrizedInitializedFilterWithoutTelemetryClient() throws ServletException, NoSuchFieldException, IllegalAccessException {
        return createInitializedParametrizedFilterWithMockTelemetryClient(false, false);
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


    /*For Parametrized constructor testing*/
    private FilterAndTelemetryClientMock createInitializedParametrizedFilterWithMockTelemetryClient(boolean withTelemetryClient, boolean clientThrows) throws ServletException, NoSuchFieldException, IllegalAccessException {
        Filter filter = createParametrizedInitializedFilter("My-App");


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