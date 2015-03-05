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

package com.microsoft.applicationinsights.web.spring;

import javax.servlet.http.HttpServletRequest;

import com.microsoft.applicationinsights.web.spring.internal.RequestNameHandlerInterceptorAdapter;
import org.eclipse.jetty.http.HttpMethods;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.web.method.HandlerMethod;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by yonisha on 3/4/2015.
 */
public class RequestNameHandlerInterceptorAdapterTests {

    private static final String DEFAULT_ACTION_NAME = "toString";
    private final String DEFAULT_CONTROLLER_NAME = this.getClass().getSimpleName();
    private HandlerMethod handlerMethod;
    private RequestNameHandlerInterceptorAdapter interceptorAdapter = new RequestNameHandlerInterceptorAdapter();

    @Before
    public void testInitialize() throws NoSuchMethodException {
        handlerMethod = new HandlerMethod(this, DEFAULT_ACTION_NAME);
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);
    }

    @Test
    public void testAdapterSetRequestNameCorrectly() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(HttpMethods.GET);

        interceptorAdapter.preHandle(request, null, handlerMethod);

        String requestName = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getName();

        String expectedRequestName =
                String.format("%s %s/%s", HttpMethods.GET, DEFAULT_CONTROLLER_NAME, DEFAULT_ACTION_NAME);

        Assert.assertEquals(expectedRequestName, requestName);
    }

    @Test
    public void testAdapterReturnTrueWhenExceptionIsThrown() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception("FATAL!");
            }
        }).when(request).getMethod();

        boolean result = interceptorAdapter.preHandle(request, null, handlerMethod);

        Assert.assertTrue("Adapter should return true.", result);
    }

    @Test
    public void testAdapterReturnTrueWhenContextNull() throws Exception {
        ThreadContext.setRequestTelemetryContext(null);
        boolean result = interceptorAdapter.preHandle(null, null, handlerMethod);

        Assert.assertTrue("Adapter should return true.", result);
    }
}
