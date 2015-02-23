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

package com.microsoft.applicationinsights.web.extensibility.modules;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.cookies.UserCookie;

/**
 * Created by yonisha on 2/7/2015.
 */
public class WebUserTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule {

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
     *
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onBeginRequest(ServletRequest req, ServletResponse res) {
        HttpServletRequest request = (HttpServletRequest)req;
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

        UserCookie userCookie =
                com.microsoft.applicationinsights.web.internal.cookies.Cookie.getCookie(
                        UserCookie.class, request, UserCookie.COOKIE_NAME);

        if (userCookie == null) {
            userCookie = new UserCookie();
        }

        context.setUserCookie(userCookie);
        UserContext userContext = context.getHttpRequestTelemetry().getContext().getUser();
        userContext.setId(userCookie.getUserId());
        userContext.setAcquisitionDate(userCookie.getAcquisitionDate());

        if (context.getUserCookie().isNewUser()) {
            setUserCookie(res, context);
        }
    }

    /**
     * End request processing.
     *
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onEndRequest(ServletRequest req, ServletResponse res) {
    }

    // endregion Public

    // region Private

    /**
     * Sets the user cookie.
     * @param res The servlet response.
     * @param context The context.
     */
    private void setUserCookie(ServletResponse res, RequestTelemetryContext context) {
        String formattedCookie = UserCookie.formatCookie(new String[] {
                context.getUserCookie().getUserId(),
                DateTimeUtils.formatAsRoundTripDate(context.getUserCookie().getAcquisitionDate())
        });

        Cookie cookie = new Cookie(UserCookie.COOKIE_NAME, formattedCookie);
        cookie.setMaxAge(Integer.MAX_VALUE);

        HttpServletResponse response = (HttpServletResponse)res;
        response.addCookie(cookie);
    }

    // endregion Private
}