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

package com.microsoft.applicationinsights.web.internal.cookies;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by yonisha on 2/4/2015.
 */
public class CookieUtils {

    // region Public

    /**
     * Gets the session cookie from the given http request.
     * @param request The request containing the cookie.
     * @return The session cookie, or null if not exists.
     */
    public static SessionCookie getSessionCookie(HttpServletRequest request) {
        Cookie cookie = getCookie(request, SessionCookie.COOKIE_NAME);

        SessionCookie sessionCookie = null;
        if (cookie != null) {
            try {
                sessionCookie = new SessionCookie(cookie);
            } catch (Exception e) {
            }
        }

        return sessionCookie;
    }

    /**
     * Gets the user cookie from the given http request.
     * @param request The request containing the cookie.
     * @return The user cookie, or null if not exists.
     */
    public static UserCookie getUserCookie(HttpServletRequest request) {
        Cookie cookie = getCookie(request, UserCookie.COOKIE_NAME);

        UserCookie userCookie = null;
        if (cookie != null) {
            try {
                userCookie = new UserCookie(cookie);
            } catch (Exception e) {
            }
        }

        return userCookie;
    }

    // endregion Public

    // region Private

    private CookieUtils(){
    }

    /**
     * Gets the cookie given the http request and cookie name.
     * @param request The http request.
     * @param cookieName The cookie name.
     * @return The cookie, or null if not exists.
     */
    private static Cookie getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                return cookie;
            }
        }

        return null;
    }

    // endregion Private
}
