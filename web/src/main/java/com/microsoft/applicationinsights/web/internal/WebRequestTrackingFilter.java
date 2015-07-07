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

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;

/**
 * The file is a wrapper around the {#link WebRequestTrackingFilterImpl}
 * There is no way to get the instance of a filter from the filter chain
 * which is needed by the {@link com.microsoft.applicationinsights.web.internal.WebAppInitializer},
 * Therefore, the class is holding a static reference to the implementation
 * and by doing so we have a way to get to the implementation and pass data to it.
 *
 * Created by gupele on 5/12/2015.
 */
public final class WebRequestTrackingFilter implements Filter {
    private static WebRequestTrackingFilterImpl impl;
    private static String name;

    public WebRequestTrackingFilter() {
        initialize();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        impl.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        impl.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {
        impl.destroy();
    }

    public static void setName(String name) {
        if (impl != null) {
            impl.setKey(name);
        } else {
            WebRequestTrackingFilter.name = name;
        }
    }

    private synchronized void initialize() {
        if (impl == null) {
            impl = new WebRequestTrackingFilterImpl();
            if (name != null) {
                setName(name);
            }
        }
    }
}
