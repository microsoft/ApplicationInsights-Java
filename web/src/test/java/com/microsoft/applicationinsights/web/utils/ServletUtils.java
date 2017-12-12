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

package com.microsoft.applicationinsights.web.utils;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Map;
import com.microsoft.applicationinsights.web.internal.WebModulesContainer;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by yonisha on 2/3/2015.
 */
public class ServletUtils {

    private ServletUtils() {
    }

    public static WebModulesContainer setMockWebModulesContainer(Filter filter) {
        WebModulesContainer container = mock(WebModulesContainer.class);

        Field field = null;
        try {
            field = getFilterWebModulesContainersField(filter);
            field.set(filter, container);
        } catch (Exception e) {
            container = null;
        }

        return container;
    }

    public static WebModulesContainer getWebModuleContainer(Filter filter) {
        WebModulesContainer container = null;

        try {
            Field field = getFilterWebModulesContainersField(filter);
            container = (WebModulesContainer)field.get(filter);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }

        return container;
    }

    public static ServletRequest generateDummyServletRequest() {
        return mock(HttpServletRequest.class);
    }

    public static ServletResponse generateDummyServletResponse() {
        return mock(HttpServletResponse.class);
    }

    public static HttpServletRequest createServletRequestWithHeaders(Map<String, String> headers) {
        return createServletRequestWithHeaders(headers, 0);
    }

    public static HttpServletRequest createServletRequestWithHeaders(Map<String, String> headers, final int correlationContextHeaderCount) {
        HttpServletRequest request = mock(HttpServletRequest.class);

        for (String headerName : headers.keySet()) {
            when(request.getHeader(headerName)).thenReturn(headers.get(headerName));
        }

        when(request.getRequestURI()).thenReturn("/controller/action.action");
        when(request.getMethod()).thenReturn("POST");
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("contoso.com");

        when(request.getHeaders(TelemetryCorrelationUtils.CORRELATION_CONTEXT_HEADER_NAME)).thenReturn(
            new Enumeration<String>() {

                private int itemCount = correlationContextHeaderCount;
                private String item1 = "key1=value1, key2=value2";
                private String item2 = "key3=value3";

				@Override
				public boolean hasMoreElements() {
                    return itemCount > 0;
				}

				@Override
				public String nextElement() {
					if (itemCount == 2) {
                        itemCount--;
                        return item2;
                    } else if (itemCount == 1) {
                        itemCount--;
                        return item1;
                    } else {
                        return null;
                    }
				}

            }
        );

        return request;
    }

    // region Private

    private static Field getFilterWebModulesContainersField(Filter filter) throws NoSuchFieldException {
        Field field = filter.getClass().getDeclaredField("webModulesContainer");
        field.setAccessible(true);

        return field;
    }

    // endregion Private
}
