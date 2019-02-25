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

package com.microsoft.applicationinsights.web.extensibility.modules;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.cookies.SessionCookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by yonisha on 2/4/2015.
 */
public class WebSessionTrackingTelemetryModule implements WebTelemetryModule<HttpServletRequest, HttpServletResponse>, TelemetryModule{

    // region Public

    /**
     * Initializes the telemetry module.
     *
     * @param configuration The configuration to used to initialize the module.
     */
    @Override
    public void initialize(TelemetryConfiguration configuration) {
    }

    /**
     * Begin request processing.
     *  @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onBeginRequest(HttpServletRequest req, HttpServletResponse res) {
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

        SessionCookie sessionCookie =
            com.microsoft.applicationinsights.web.internal.cookies.Cookie.getCookie(
                SessionCookie.class, req, SessionCookie.COOKIE_NAME);

        if (sessionCookie == null) {
            return;
        }

        context.setSessionCookie(sessionCookie);

        String sessionId = sessionCookie.getSessionId();
        getTelemetrySessionContext(context).setId(sessionId);
    }

    /**
     * End request processing.
     * This method checks if the session cookie should be updated before sent back to the client.
     * The session cookie is updated when the session is new or current session already expired.
     *
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onEndRequest(HttpServletRequest req, HttpServletResponse res) {
    }

    // endregion Public

    // region Private

    private SessionContext getTelemetrySessionContext(RequestTelemetryContext aiContext) {
        return aiContext.getHttpRequestTelemetry().getContext().getSession();
    }

    // endregion Private
}