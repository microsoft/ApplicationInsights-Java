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

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;

/**
 * Created by yonisha on 2/26/2015.
 */
public class HttpCookieFactory {

    public static String COOKIE_PATH_ALL_URL = "/";

    // region Public

    /**
     * Generates session http cookie.
     * @param context The context.
     * @param sessionContext The request session context.
     * @param sessionTimeoutInMinutes The session timeout in minutes.
     * @return Session http cookie.
     */
    public static Cookie generateSessionHttpCookie(
            RequestTelemetryContext context, SessionContext sessionContext, int sessionTimeoutInMinutes) {
        Date renewalDate = DateTimeUtils.getDateTimeNow();
        Date expirationDate = DateTimeUtils.addToDate(
                renewalDate,
                Calendar.MINUTE,
                sessionTimeoutInMinutes);
        long timeDiffInSeconds = DateTimeUtils.getDateDiff(expirationDate, DateTimeUtils.getDateTimeNow(), TimeUnit.SECONDS);

        String formattedCookie = SessionCookie.formatCookie(new String[] {
                sessionContext.getId(),
                DateTimeUtils.formatAsRoundTripDate(context.getSessionCookie().getSessionAcquisitionDate()),
                DateTimeUtils.formatAsRoundTripDate(renewalDate)
        });

        Cookie cookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

        cookie.setMaxAge((int)timeDiffInSeconds);

        setCommonProperties(cookie);

        return cookie;
    }

    /**
     * Generates user http cookie.
     * @param context The context.
     * @return User http cookie.
     */
    public static Cookie generateUserHttpCookie(RequestTelemetryContext context) {
        String formattedCookie = UserCookie.formatCookie(new String[] {
                context.getUserCookie().getUserId(),
                DateTimeUtils.formatAsRoundTripDate(context.getUserCookie().getAcquisitionDate())
        });

        Cookie cookie = new Cookie(UserCookie.COOKIE_NAME, formattedCookie);
        cookie.setMaxAge(Integer.MAX_VALUE);

        setCommonProperties(cookie);

        return cookie;
    }

    // endregion Public

    // region Private

    private static void setCommonProperties(Cookie cookie) {
        cookie.setPath(COOKIE_PATH_ALL_URL);
    }

    // endregion Private
}
