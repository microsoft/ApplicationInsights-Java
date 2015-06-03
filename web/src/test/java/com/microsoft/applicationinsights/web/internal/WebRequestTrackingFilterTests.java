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

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Created by yonisha on 2/3/2015.
 */
public final class WebRequestTrackingFilterTests {
    private static final class FilterAndImpl {
        public final WebRequestTrackingFilter filter;
        public final WebRequestTrackingFilterImpl impl;

        private FilterAndImpl(WebRequestTrackingFilter filter, WebRequestTrackingFilterImpl impl) {
            this.filter = filter;
            this.impl = impl;
        }
    }

    @Test
    public void testInitIsCalledOnce() throws NoSuchFieldException, IllegalAccessException, ServletException {
        FilterAndImpl filterAndImpl = createFilterAndImpl();

        FilterConfig mockConfig = Mockito.mock(FilterConfig.class);
        filterAndImpl.filter.init(mockConfig);

        Mockito.verify(filterAndImpl.impl, times(1)).init(mockConfig);
    }

    @Test
    public void testDoFilterIsCalledOnce() throws NoSuchFieldException, IllegalAccessException, ServletException, IOException {
        FilterAndImpl filterAndImpl = createFilterAndImpl();

        ServletRequest mockRequest = Mockito.mock(ServletRequest.class);
        ServletResponse mockResponse = Mockito.mock(ServletResponse.class);
        FilterChain mockChain = Mockito.mock(FilterChain.class);
        filterAndImpl.filter.doFilter(mockRequest, mockResponse, mockChain);

        Mockito.verify(filterAndImpl.impl, times(1)).doFilter(mockRequest, mockResponse, mockChain);
    }

    @Test
    public void testDestroyIsCalledOnce() throws NoSuchFieldException, IllegalAccessException, ServletException, IOException {
        FilterAndImpl filterAndImpl = createFilterAndImpl();

        filterAndImpl.filter.destroy();

        Mockito.verify(filterAndImpl.impl, times(1)).destroy();
    }

    private static FilterAndImpl createFilterAndImpl() throws NoSuchFieldException, IllegalAccessException {
        Field f = WebRequestTrackingFilter.class.getDeclaredField("impl");
        f.setAccessible(true);

        WebRequestTrackingFilterImpl mockFilterImpl = Mockito.mock(WebRequestTrackingFilterImpl.class);
        WebRequestTrackingFilter tested = new WebRequestTrackingFilter();
        f.set(tested, mockFilterImpl);

        return new FilterAndImpl(tested, mockFilterImpl);
    }
    // endregion Private methods
}
