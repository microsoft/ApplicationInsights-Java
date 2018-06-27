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

package com.microsoft.applicationinsights.web.struts;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Created by yonisha on 3/10/2015. */
public class RequestNameInterceptorTests {

  private static final String HTTP_METHOD = "GET";
  private static final String ACTION_NAME = "TestAction";
  private static final String REQUEST_NAME = String.format("%s /%s", HTTP_METHOD, ACTION_NAME);

  private static ActionContext actionContext;
  private static ActionInvocation actionInvocation;
  private static RequestNameInterceptor interceptor = new RequestNameInterceptor();

  @BeforeClass
  public static void classInitialize() {
    actionInvocation = mock(ActionInvocation.class);
  }

  @Before
  public void testInitialize() {
    RequestTelemetryContext requestTelemetryContext =
        new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
    ThreadContext.setRequestTelemetryContext(requestTelemetryContext);
    RequestTelemetry requestTelemetry =
        ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
    requestTelemetry.setHttpMethod(HTTP_METHOD);

    // Setting mock for action context
    actionContext = mock(ActionContext.class);
    when(actionContext.getName()).thenReturn(ACTION_NAME);
    ActionContext.setContext(actionContext);
  }

  @Test
  public void testInterceptorSetRequestNameCorrectly() throws Exception {
    interceptor.intercept(actionInvocation);

    RequestTelemetry requestTelemetry =
        ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
    Assert.assertEquals(REQUEST_NAME, requestTelemetry.getName());
  }

  @Test
  public void testActionInvocationWhenExceptionThrownDuringRequestCalculation() throws Exception {

    // Mocking the ActionContext to throw exception.
    Mockito.doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception("FATAL!");
              }
            })
        .when(actionContext)
        .getName();
    ActionContext.setContext(actionContext);

    interceptor.intercept(actionInvocation);
  }
}
