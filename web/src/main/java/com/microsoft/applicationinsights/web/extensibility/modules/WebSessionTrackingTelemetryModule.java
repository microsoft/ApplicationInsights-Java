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

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.cookies.SessionCookie;

/**
 * Created by yonisha on 2/4/2015.
 */
public class WebSessionTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule{

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

        SessionCookie sessionCookie =
                com.microsoft.applicationinsights.web.internal.cookies.Cookie.getCookie(
                        SessionCookie.class, request, SessionCookie.COOKIE_NAME);

        if (sessionCookie != null && !sessionCookie.isSessionExpired()) {
            // Update ai context with session details.
            getTelemetrySessionContext(context).setId(sessionCookie.getSessionId());
            context.setSessionCookie(sessionCookie);
        } else {
            startNewSession(context);
        }

        setSessionCookie(request, (HttpServletResponse)res);
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
    public void onEndRequest(ServletRequest req, ServletResponse res) {
    }

    // endregion Public

    // region Private

    private void setSessionCookie(HttpServletRequest req, HttpServletResponse res) {
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();
        if (isSessionCookieUpToDate(context)) {
            return;
        }

        Date renewalDate = DateTimeUtils.getDateTimeNow();

        String formattedCookie = SessionCookie.formatCookie(new String[] {
                getTelemetrySessionContext(context).getId(),
                DateTimeUtils.formatAsRoundTripDate(context.getSessionCookie().getSessionAcquisitionDate()),
                DateTimeUtils.formatAsRoundTripDate(renewalDate)
        });

        Cookie cookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

        Date expirationDate = DateTimeUtils.addToDate(
                renewalDate,
                Calendar.MINUTE,
                SessionCookie.SESSION_DEFAULT_EXPIRATION_TIMEOUT_IN_MINUTES);
        long timeDiffInSeconds = DateTimeUtils.getDateDiff(expirationDate, DateTimeUtils.getDateTimeNow(), TimeUnit.SECONDS);

        cookie.setMaxAge((int)timeDiffInSeconds);
        cookie.setPath("/");

        res.addCookie(cookie);
    }

    private SessionContext getTelemetrySessionContext(RequestTelemetryContext aiContext) {
        return aiContext.getHttpRequestTelemetry().getContext().getSession();
    }

    private void startNewSession(RequestTelemetryContext aiContext) {
        String sessionId = UUID.randomUUID().toString();

        SessionContext session = getTelemetrySessionContext(aiContext);
        session.setId(sessionId);
        session.setIsNewSession(true);

        try {
            aiContext.setSessionCookie(new SessionCookie(sessionId));
        } catch (Exception e) {
            // TODO: change when creating dedicated parse exception.
            // This exception is not expected in any case.
            InternalLogger.INSTANCE.error("Failed to create session cookie with error: %s", e.getMessage());
        }
    }

    private boolean isSessionCookieUpToDate(RequestTelemetryContext context) {
        boolean isNewSession = getTelemetrySessionContext(context).getIsNewSession();

        SessionCookie sessionCookie = context.getSessionCookie();
        boolean isExpiredSession = sessionCookie == null || sessionCookie.isSessionExpired();

        return !isNewSession && !isExpiredSession;
    }

    // endregion Private
}
