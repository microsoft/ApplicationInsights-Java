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
import com.microsoft.applicationinsights.web.internal.httputils.ApplicationInsightsServletExtractor;
import com.microsoft.applicationinsights.web.internal.httputils.HttpExtractor;
import com.microsoft.applicationinsights.web.internal.httputils.HttpServerHandler;
import org.apache.http.HttpRequest;
import org.junit.Assert;
import org.junit.Test;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.microsoft.applicationinsights.web.utils.ServletUtils;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Created by yonisha on 2/3/2015.
 */
public class WebRequestTrackingFilterTests {

    private String mockContextPath = "/FakeContext";

    private static class FilterAndTelemetryClientMock {
        public final Filter filter;
        public final TelemetryClient mockTelemetryClient;
        final HttpServerHandler<HttpServletRequest, HttpServletResponse> handler;

        private FilterAndTelemetryClientMock(Filter filter, TelemetryClient mockTelemetryClient,
            HttpServerHandler<HttpServletRequest, HttpServletResponse> handler) {
            this.filter = filter;
            this.mockTelemetryClient = mockTelemetryClient;
            this.handler = handler;

        }
    }

    @Test
    public void testFilterInitializedSuccessfullyFromConfiguration() throws ServletException {
        Filter filter = createInitializedFilter();
        WebModulesContainer container = ServletUtils.getWebModuleContainer(filter);

        assertNotNull("Container shouldn't be null", container);
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

    @Test
    public void testWebAppNameInitializerAppNameHandoff() throws Exception {
        FilterAndTelemetryClientMock mocks = createInitializedFilterWithExposedTelemetryClient();
        FilterChain chain = mock(FilterChain.class);

        mocks.filter.doFilter(ServletUtils.generateDummyServletRequest(), ServletUtils.generateDummyServletResponse(), chain);
        String role = mocks.mockTelemetryClient.getContext().getCloud().getRole();
        assertNotNull(role);
        assertEquals(this.mockContextPath.replace("/", ""), role);

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
                verify(createdData.mockTelemetryClient, times(1)).trackException(any(Exception.class));
            }
        }
    }

    private Filter createInitializedFilter() throws ServletException {
        Filter filter = new WebRequestTrackingFilter();
        FilterConfig config = mock(FilterConfig.class);
        when(config.getFilterName()).thenReturn(WebRequestTrackingFilter.FILTER_NAME);

        ServletContext context = mock(ServletContext.class);

        when(context.getContextPath()).thenReturn(this.mockContextPath);

        when(config.getServletContext()).thenReturn(context);
        filter.init(config);

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
        Field field1 = WebRequestTrackingFilter.class.getDeclaredField("handler");
        field.setAccessible(true);

        TelemetryClient mockTelemetryClient = null;

        if (withTelemetryClient) {
            mockTelemetryClient = spy(new TelemetryClient());
            if (clientThrows) {
                doThrow(new RuntimeException()).when(mockTelemetryClient).trackException(any(Exception.class));
            }
        }
        HttpExtractor<HttpServletRequest, HttpServletResponse> extractor = spy(new ApplicationInsightsServletExtractor());
        HttpServerHandler<HttpServletRequest, HttpServletResponse> handler = new HttpServerHandler<>(
            extractor, mock(WebModulesContainer.class), mockTelemetryClient
        );
        field.set(filter, mockTelemetryClient);
        field1.set(filter,  handler);

        return new FilterAndTelemetryClientMock(filter, mockTelemetryClient, handler);
    }

    private FilterAndTelemetryClientMock createInitializedFilterWithExposedTelemetryClient() throws Exception {
        Filter filter = createInitializedFilter();
        Field field = WebRequestTrackingFilter.class.getDeclaredField("telemetryClient");
        field.setAccessible(true);
        return new FilterAndTelemetryClientMock(filter, (TelemetryClient) field.get(filter), null);
    }
    // endregion Private methods
}