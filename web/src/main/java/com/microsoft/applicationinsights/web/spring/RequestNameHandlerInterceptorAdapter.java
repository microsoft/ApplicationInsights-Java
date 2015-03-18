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
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * Created by yonisha on 3/1/2015.
 *
 * This class intercepts all requests before forwarded to the relevant handler.
 * It extracts the relevant controller and method names and updates the request telemetry name.
 * This class should never throw exceptions to avoid blocking request processing.
 */
public class RequestNameHandlerInterceptorAdapter extends HandlerInterceptorAdapter {

    // region Public

    /**
     * This method is being invoked just before the request is forwarded the the relevant controller.
     * It should always return true, so the request will be processed in any case of failure.
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        try {
            RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

            if (context == null) {
                return true;
            }

            String requestName = generateRequestName(request, handler);

            if (requestName == null) {
                return true;
            }

            context.getHttpRequestTelemetry().setName(requestName);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                "Failed to invoke interceptor '%s' with exception: %s.",
                this.getClass().getSimpleName(),
                e.getMessage());
        }

        return true;
    }

    // endregion Public

    // region Private

    private String generateRequestName(HttpServletRequest request, Object handler) {

        // Some handlers, such as built-in ResourceHttpRequestHandler are not of type HandlerMethod.
        if (!(handler instanceof HandlerMethod)) {
            return null;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        String controller = handlerMethod.getBeanType().getSimpleName();
        String action = handlerMethod.getMethod().getName();

        String method = request.getMethod();

        return String.format("%s %s/%s", method, controller, action);
    }

    // endregion Private
}

