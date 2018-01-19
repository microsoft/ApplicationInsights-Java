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

package com.microsoft.applicationinsights.web.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.internal.cookies.SessionCookie;
import com.microsoft.applicationinsights.web.internal.cookies.UserCookie;

import javax.servlet.http.Cookie;

/**
 * Created by yonisha on 2/2/2015.
 */
public class HttpHelper {

    private static final String COOKIE = "00000000-0000-0000-0000-000000000000";
    private static final String FORMATTED_USER_COOKIE_TEMPLATE = COOKIE + "|%s";
    private static final String FORMATTED_SESSION_COOKIE_TEMPLATE = "00000000-0000-0000-0000-000000000000|%s|%s";
    public static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 6.4; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko";

    public static CookiesContainer sendRequestAndGetResponseCookie(int portNumber, String... requestFormattedCookies) throws Exception {
        List<String> responseCookies = sendRequestAndGetHeaders(portNumber, requestFormattedCookies).get("Set-Cookie");
        CookiesContainer cookiesContainer = getCookiesContainer(responseCookies);
        return cookiesContainer;
    }

    public static Map<String, List<String>> sendRequestAndGetHeaders(int portNumber, String... requestFormattedCookies) throws Exception {
        HttpURLConnection con = (HttpURLConnection) (new URL("http://localhost:" + portNumber)).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", TEST_USER_AGENT);

        for (String requestFormattedCookie : requestFormattedCookies) {
            con.setRequestProperty("Cookie", requestFormattedCookie);
        }

        con.getResponseCode();

        return con.getHeaderFields();
    }

    public static String getCookie() {
        return COOKIE;
    }

    public static CookiesContainer sendRequestAndGetResponseCookie(int portNumber) throws Exception {
        return sendRequestAndGetResponseCookie(portNumber, new String[] {});
    }

    public static String getFormattedUserCookieHeader() {
        String formattedUserCookie = null;

        try {
            formattedUserCookie = String.format(
                    FORMATTED_USER_COOKIE_TEMPLATE,
                    DateTimeUtils.formatAsRoundTripDate(DateTimeUtils.getDateTimeNow()));
        } catch (Exception e) {
        }

        return String.format("%s=%s", UserCookie.COOKIE_NAME, formattedUserCookie);
    }

    public static String getFormattedSessionCookieHeader(boolean expired) {
        String formattedSessionCookie = null;

        try {
            formattedSessionCookie = getFormattedSessionCookie(expired);
        } catch (Exception e) {
        }

        return formattedSessionCookie;
    }

    public static String getFormattedSessionCookie(boolean expired) throws ParseException {
        return getFormattedSessionCookieWithOldTime(expired ? 500 : 0);
    }

    public static String getFormattedSessionCookieWithOldTime(int timeInMinutes) {
        Date sessionAcquisitionTime = new Date();
        sessionAcquisitionTime = DateTimeUtils.addToDate(sessionAcquisitionTime, Calendar.MINUTE, -timeInMinutes);

        Date sessionRenewalTime = DateTimeUtils.addToDate(sessionAcquisitionTime, Calendar.SECOND, 1);

        String formattedSessionCookie = String.format(
                FORMATTED_SESSION_COOKIE_TEMPLATE,
                String.valueOf(sessionAcquisitionTime.getTime()),
                String.valueOf(sessionRenewalTime.getTime()));

        return String.format("%s=%s", SessionCookie.COOKIE_NAME, formattedSessionCookie);
    }

    public static String getSessionIdFromCookie(String cookie) {
        Pattern pattern = Pattern.compile("(.*)=(.*)\\|(.*)\\|(.*)");

        String sessionId = null;
        Matcher matcher = pattern.matcher(cookie);
        if (matcher.matches()) {
            sessionId = matcher.group(2);
        }

        return sessionId;
    }

    private static CookiesContainer getCookiesContainer(List<String> responseCookies) throws Exception {
        CookiesContainer cookiesContainer = new CookiesContainer();

        if (responseCookies == null) {
            return cookiesContainer;
        }

        for (String formattedCookieWithExpiration : responseCookies) {
            if (formattedCookieWithExpiration.startsWith("ai_user")) {
                String formattedCookie = formattedCookieWithExpiration.split("=")[1].split(";")[0];
                Cookie cookie = new Cookie(UserCookie.COOKIE_NAME, formattedCookie);

                UserCookie userCookie = new UserCookie(cookie);
                cookiesContainer.setUserCookie(userCookie);
            } else if(formattedCookieWithExpiration.startsWith("ai_session")) {
                String formattedCookie = formattedCookieWithExpiration.split("=")[1].split(";")[0];
                Cookie cookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

                SessionCookie sessionCookie = new SessionCookie(cookie);
                cookiesContainer.setSessionCookie(sessionCookie);
            }
        }

        return cookiesContainer;
    }
}