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
package com.microsoft.applicationinsights.web.javaee;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 * @author Daichi Isami
 */
@Interceptor
@RequestName
public class RequestNameInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext ic) throws Exception {
        setRequestNameSafe(ic);
        return ic.proceed();
    }

    private void setRequestNameSafe(InvocationContext ic) {
        try {
            RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();
            if (context != null) {

                String actionName = String.format("%s.%s", ic.getMethod().getDeclaringClass().getName(), ic.getMethod().getName());
                String httpMethod = context.getHttpRequestTelemetry().getHttpMethod();
                String requestName = String.format("%s %s", httpMethod, actionName);
                
                context.getHttpRequestTelemetry().setName(requestName);
            }
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to invoke interceptor '%s' with exception: %s.",
                    this.getClass().getSimpleName(),
                    e.getMessage());
            InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
        }
    }
}
