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

package com.microsoft.applicationinsights.web.struts;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

/**
 * Created by yonisha on 3/10/2015.
 */
public class RequestNameInterceptor implements Interceptor {

    // region Public

    /**
     * Called after an interceptor is created, but before any requests are processed using
     * {@link #intercept(com.opensymphony.xwork2.ActionInvocation) intercept} , giving
     * the Interceptor a chance to initialize any needed resources.
     */
    @Override
    public void init() {
    }

    /**
     * Allows the Interceptor to do some processing on the request before and/or after the rest of the processing of the
     * request by the {@link com.opensymphony.xwork2.ActionInvocation} or to short-circuit the processing and just return a String return code.
     *
     * @param invocation The action invocation.
     * @return the return code, either returned from {@link com.opensymphony.xwork2.ActionInvocation#invoke()}, or from the interceptor itself.
     * @throws Exception any system-level error, as defined in {@link com.opensymphony.xwork2.Action#execute()}.
     */
    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        setRequestNameSafe();

        String result = invocation.invoke();

        return result;
    }

    /**
     * Called to let an interceptor clean up any resources it has allocated.
     */
    @Override
    public void destroy() {
    }

    // endregion Public

    // region Private

    private void setRequestNameSafe() {
        try {
            RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

            if (context != null) {

                // Here we also rely on the HttpRequestTelemetry information to extract the http method,
                // which already set during the web request tracking filter execution.
                String actionName = ActionContext.getContext().getName();
                String httpMethod = context.getHttpRequestTelemetry().getHttpMethod();
                String requestName = String.format("%s /%s", httpMethod, actionName);

                context.getHttpRequestTelemetry().setName(requestName);
            }
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to invoke interceptor '%s' with exception: %s.",
                    this.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    // endregion Private
}
