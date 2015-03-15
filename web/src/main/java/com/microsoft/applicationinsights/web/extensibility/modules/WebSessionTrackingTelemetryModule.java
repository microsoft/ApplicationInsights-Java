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

import java.util.UUID;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.SessionState;
import com.microsoft.applicationinsights.telemetry.SessionStateTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ServletUtils;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.cookies.HttpCookieFactory;
import com.microsoft.applicationinsights.web.internal.cookies.SessionCookie;
import com.microsoft.applicationinsights.web.internal.cookies.UserCookie;

/**
 * Created by yonisha on 2/4/2015.
 */
public class WebSessionTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule{

    // region Members

    private TelemetryClient telemetryClient;
    private boolean isInitialized = false;
    private boolean isUserModuleEnabled = false;

    // endregion Members

    // region Public

    /**
     * Initializes the telemetry module.
     *
     * @param configuration The configuration to used to initialize the module.
     */
    @Override
    public void initialize(TelemetryConfiguration configuration) {
        try {
            telemetryClient = new TelemetryClient(configuration);
            isInitialized = true;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to initialize telemetry module " + this.getClass().getSimpleName() + ". Exception: %s.", e.getMessage());
        }
    }

    /**
     * Begin request processing.
     *
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onBeginRequest(ServletRequest req, ServletResponse res) {
        if (!isInitialized) {
            // Avoid logging to not spam the log. It is sufficient that the module initialization failure
            // has been logged.
            return;
        }

        HttpServletRequest request = (HttpServletRequest)req;
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

        boolean isNewUser = isNewUser(request);

        if (isNewUser && isUserModuleEnabled) {
            context.getHttpRequestTelemetry().getContext().getSession().setIsFirst(true);
        }

        SessionCookie sessionCookie =
                com.microsoft.applicationinsights.web.internal.cookies.Cookie.getCookie(
                        SessionCookie.class, request, SessionCookie.COOKIE_NAME);

        boolean startNewSession = false;
        if (sessionCookie == null) {
            startNewSession = true;
        } else {
            if (sessionCookie.isSessionExpired()) {
                startNewSession = true;
                trackSessionStateWithRequestSessionId(SessionState.End, sessionCookie.getSessionId());
            } else {
                // Update ai context with session details.
                getTelemetrySessionContext(context).setId(sessionCookie.getSessionId());
                context.setSessionCookie(sessionCookie);
            }
        }

        if (startNewSession) {
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

    /**
     * Sets a value indicating whether the user tracking module enabled.
     * @param isUserModuleEnabled True if the user module enabled, false otherwise.
     */
    public void setIsUserModuleEnabled(boolean isUserModuleEnabled){
        this.isUserModuleEnabled = isUserModuleEnabled;
    }

    /**
     * Gets a value indicating whether the user tracking module enabled.
     * @return True if the user module enabled, false otherwise.
     */
    public boolean getIsUserModuleEnabled(){
        return isUserModuleEnabled;
    }

    // endregion Public

    // region Private

    private void setSessionCookie(HttpServletRequest req, HttpServletResponse res) {
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();
        if (isSessionCookieUpToDate(context)) {
            return;
        }

        int sessionTimeout = getSessionTimeout(req);
        SessionContext sessionContext = getTelemetrySessionContext(context);
        Cookie cookie = HttpCookieFactory.generateSessionHttpCookie(context, sessionContext, sessionTimeout);

        res.addCookie(cookie);
    }

    private int getSessionTimeout(ServletRequest servletRequest) {
        Integer sessionTimeout = ServletUtils.getRequestSessionTimeout(servletRequest);
        if (sessionTimeout == null) {
            sessionTimeout = SessionCookie.SESSION_DEFAULT_EXPIRATION_TIMEOUT_IN_MINUTES;
        }

        return sessionTimeout;
    }

    private SessionContext getTelemetrySessionContext(RequestTelemetryContext aiContext) {
        return aiContext.getHttpRequestTelemetry().getContext().getSession();
    }

    private void startNewSession(RequestTelemetryContext aiContext) {
        String sessionId = UUID.randomUUID().toString();

        SessionContext session = getTelemetrySessionContext(aiContext);
        session.setId(sessionId);

        aiContext.setSessionCookie(new SessionCookie(sessionId));
        trackSessionStateWithRequestSessionId(SessionState.Start, sessionId);
        aiContext.setIsNewSession(true);
    }

    private boolean isSessionCookieUpToDate(RequestTelemetryContext context) {
        boolean isNewSession = context.getIsNewSession();

        SessionCookie sessionCookie = context.getSessionCookie();
        boolean isExpiredSession = sessionCookie == null || sessionCookie.isSessionExpired();

        return !isNewSession && !isExpiredSession;
    }

    private void trackSessionStateWithRequestSessionId(SessionState requiredState, String sessionId) {
        SessionStateTelemetry sessionStateTelemetry = new SessionStateTelemetry(requiredState);
        sessionStateTelemetry.getContext().getSession().setId(sessionId);

        telemetryClient.track(sessionStateTelemetry);
    }

    private boolean isNewUser(HttpServletRequest request) {
        UserCookie userCookie = com.microsoft.applicationinsights.web.internal.cookies.Cookie.getCookie(
                UserCookie.class, request, UserCookie.COOKIE_NAME);

        return userCookie == null;
    }

    // endregion Private
}
