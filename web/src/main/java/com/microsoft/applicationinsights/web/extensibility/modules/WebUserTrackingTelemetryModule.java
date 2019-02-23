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

import java.util.Date;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.cookies.UserCookie;

/**
 * Created by yonisha on 2/7/2015.
 */
public class WebUserTrackingTelemetryModule implements WebTelemetryModule<HttpServletRequest, HttpServletResponse>, TelemetryModule {

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
        HttpServletRequest request = (HttpServletRequest)req;
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

        UserCookie userCookie =
                com.microsoft.applicationinsights.web.internal.cookies.Cookie.getCookie(
                        UserCookie.class, request, UserCookie.COOKIE_NAME);

        if (userCookie == null) {
            return;
        }

        String userId = userCookie.getUserId();
        Date acquisitionDate = userCookie.getAcquisitionDate();

        context.setUserCookie(userCookie);

        UserContext userContext = context.getHttpRequestTelemetry().getContext().getUser();
        userContext.setId(userId);
        userContext.setAcquisitionDate(acquisitionDate);
    }

    /**
     * End request processing.
     *
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onEndRequest(HttpServletRequest req, HttpServletResponse res) {
    }

    // endregion Public
}