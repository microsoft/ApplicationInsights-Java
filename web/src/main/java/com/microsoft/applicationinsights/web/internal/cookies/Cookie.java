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

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * Created by yonisha on 2/9/2015.
 *
 * Used as a base class for AI cookies.
 */
public class Cookie {
    protected static final String RAW_COOKIE_DELIMITER = "|";
    protected static final String RAW_COOKIE_SPLIT_DELIMITER = "\\" + RAW_COOKIE_DELIMITER;

    /**
     * Formats the given values to a session cookie value.
     * @param values The values to format.
     * @return Formatted session cookie.
     */
    public static String formatCookie(String[] values) {
        return StringUtils.join(values, RAW_COOKIE_DELIMITER);
    }

    /**
     * Gets the cookie from the given http request.
     * @param eClass The required cookie type.
     * @param request THe http request to get the cookies from.
     * @param cookieName The cookie name.
     * @param <E> The required cookie type.
     * @return Cookie from the required type.
     */
    public static <E> E getCookie(Class<E> eClass, HttpServletRequest request, String cookieName) {
        javax.servlet.http.Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        javax.servlet.http.Cookie httpCookie = null;
        for (javax.servlet.http.Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                httpCookie = cookie;
            }
        }

        if (httpCookie == null) {

            // Http cookie hasn't been found.
            return null;
        }

        E instance = null;
        try {
            instance = eClass.getConstructor(javax.servlet.http.Cookie.class).newInstance(httpCookie);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to create cookie with error: " + e.getMessage());
        }

        return instance;
    }
}
