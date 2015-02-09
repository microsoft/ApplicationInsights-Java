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
import javax.servlet.http.Cookie;
import org.apache.commons.lang3.StringUtils;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;

/**
 * Created by yonisha on 2/4/2015.
 */
public class SessionCookie extends com.microsoft.applicationinsights.web.internal.cookies.Cookie{

    // region Consts

    private static final int RAW_COOKIE_SESSION_ID_INDEX = 0;
    private static final int RAW_COOKIE_SESSION_ACQUISITION_DATE_INDEX = 1;
    private static final int RAW_COOKIE_SESSION_LAST_UPDATE_DATE_INDEX = 2;
    private static final int RAW_COOKIE_EXPECTED_VALUES_COUNT = 3;

    public static final String COOKIE_NAME = "ai_session";
    public static final int SESSION_DEFAULT_EXPIRATION_TIMEOUT_IN_MINUTES = 30;

    // endregion Consts

    // region Members

    private String sessionId;
    private Date acquisitionDate;
    private Date renewalDate;

    // endregion Members

    // region Ctor

    /**
     * Constructs new SessionCookie object from the given cookie.
     * @param cookie The http servlet cookie.
     * @throws Exception Thrown when the cookie information cannot be parsed.
     */
    public SessionCookie(Cookie cookie) throws Exception {
        parseCookie(cookie);
    }

    /**
     * Consrtucts new SessionCookie with the given session ID.
     * @param sessionId The session ID.
     * @throws Exception Thrown when the cookie information cannot be parsed.
     */
    public SessionCookie(String sessionId) throws Exception {
        long nowTicks = DateTimeUtils.getDateTimeNow().getTime();
        String[] cookieRawValues = new String[] { sessionId, String.valueOf(nowTicks), String.valueOf(nowTicks) };
        String formattedCookie = SessionCookie.formatCookie(cookieRawValues);

        Cookie cookie = new Cookie(COOKIE_NAME, formattedCookie);

        parseCookie(cookie);
    }

    // endregion Ctor

    // region Public

    /**
     * Formats the given values to a session cookie value.
     * @param values The values to format.
     * @return Formatted session cookie.
     */
    public static String formatCookie(String[] values) {
        return StringUtils.join(values, RAW_COOKIE_DELIMITER);
    }

    /**
     * Gets the session id.
     * @return The session id.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the session acquisition date.
     * @return The session acquisition date.
     */
    public Date getSessionAcquisitionDate() {
        return acquisitionDate;
    }

    /**
     * Gets the session renewal date.
     * @return The session renewal date.
     */
    public Date getSessionRenewalDate() {
        return renewalDate;
    }

    /**
     * Determines if the session has expired.
     * @return True if the session has expired, false otherwise.
     */
    public boolean isSessionExpired() {
        Date expirationDate = DateTimeUtils.addToDate(
                this.getSessionAcquisitionDate(),
                Calendar.MINUTE,
                SESSION_DEFAULT_EXPIRATION_TIMEOUT_IN_MINUTES);
        Date now = new Date();

        return  now.after(expirationDate);
    }

    // endregion Public

    // region Private

    /**
     * Parses the given cookie.
     * @param cookie The cookie contains the session information.
     * @throws Exception Thrown when the cookie information cannot be parsed.
     */
    private void parseCookie(Cookie cookie) throws Exception {
        String[] split = cookie.getValue().split(RAW_COOKIE_SPLIT_DELIMITER);

        if (split.length < RAW_COOKIE_EXPECTED_VALUES_COUNT) {

            // TODO: dedicated exception
            String errorMessage = String.format("Session cookie is not in the correct format: %s", cookie.getValue());

            throw new Exception(errorMessage);
        }

        try {
            sessionId = split[RAW_COOKIE_SESSION_ID_INDEX];
            validateUUID(sessionId);

            acquisitionDate = new Date(Long.parseLong(split[RAW_COOKIE_SESSION_ACQUISITION_DATE_INDEX]));
            renewalDate = new Date(Long.parseLong(split[RAW_COOKIE_SESSION_LAST_UPDATE_DATE_INDEX]));
        } catch (Exception e) {
            String errorMessage = String.format("Failed to parse session cookie with exception: %s", e.getMessage());

            // TODO: dedicated exception
            throw new Exception(errorMessage);
        }
    }

    // endregion Private
}
