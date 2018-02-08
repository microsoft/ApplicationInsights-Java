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

package com.microsoft.applicationinsights.web.internal.cookies;

import javax.servlet.http.Cookie;

/**
 * Created by yonisha on 2/4/2015.
 */
public class SessionCookie extends com.microsoft.applicationinsights.web.internal.cookies.Cookie {

    // region Consts

    public static final String COOKIE_NAME = "ai_session";
    public static final int SESSION_DEFAULT_EXPIRATION_TIMEOUT_IN_MINUTES = 30;

    // endregion Consts

    // region Members

    private String sessionId;

    // endregion Members

    // region Ctor

    /**
     * Constructs new SessionCookie object from the given cookie.
     * @param cookie The http servlet cookie.
     * @throws Exception Thrown when the cookie information cannot be parsed.
     */
    public SessionCookie(Cookie cookie) {
        this(parseCookie(cookie));
    }

    /**
     * Constructs new SessionCookie with the given session ID.
     * @param sessionId The session ID.
     */
    public SessionCookie(String sessionId) {
        this.sessionId = sessionId;
    }

    // endregion Ctor

    // region Public

    /**
     * Gets the session id.
     * @return The session id.
     */
    public String getSessionId() {
        return sessionId;
    }

    // endregion Public

    // region Private

    /**
     * Parses the given cookie.
     * @param cookie The cookie contains the session information.
     * @return sessionId 
     */
    private static String parseCookie(Cookie cookie) {
        String value = cookie.getValue();
        int idx = value.indexOf(RAW_COOKIE_DELIMITER);
        if (idx >= 0) {
            return value.substring(0, idx);
        } else {
            return value;
        }
    }
    // endregion Private
}
