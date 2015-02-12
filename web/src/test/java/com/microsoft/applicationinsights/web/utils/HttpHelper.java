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

package com.microsoft.applicationinsights.web.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.internal.cookies.SessionCookie;
import com.microsoft.applicationinsights.web.internal.cookies.UserCookie;

import javax.servlet.http.Cookie;

/**
 * Created by yonisha on 2/2/2015.
 */
public class HttpHelper {

    private static final String FORMATTED_USER_COOKIE_TEMPLATE = "00000000-0000-0000-0000-000000000000|%s";
    private static final String FORMATTED_SESSION_COOKIE_TEMPLATE = "00000000-0000-0000-0000-000000000000|%s|%s";

    public static CookiesContainer sendRequestAndGetResponseCookie(String... requestFormattedCookies) throws Exception {
        HttpURLConnection con = (HttpURLConnection) (new URL("http://localhost:1234")).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.4; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko");

        for (String requestFormattedCookie : requestFormattedCookies) {
            con.setRequestProperty("Cookie", requestFormattedCookie);
        }

        con.getResponseCode();

        List<String> responseCookies = con.getHeaderFields().get("Set-Cookie");

        CookiesContainer cookiesContainer = getCookiesContainer(responseCookies);

        return cookiesContainer;
    }

    public static CookiesContainer sendRequestAndGetResponseCookie() throws Exception {
        return sendRequestAndGetResponseCookie(new String[] {});
    }

    public static String getFormattedUserCookieHeader() {
        String formattedUserCookie = String.format(
                FORMATTED_USER_COOKIE_TEMPLATE, DateTimeUtils.getDateTimeNow().getTime());

        return String.format("%s=%s", UserCookie.COOKIE_NAME, formattedUserCookie);
    }

    public static String getFormattedSessionCookieHeader(boolean expired) {
        String formattedSessionCookie = getFormattedSessionCookie(expired);

        return String.format("%s=%s", SessionCookie.COOKIE_NAME, formattedSessionCookie);
    }

    public static String getFormattedSessionCookie(boolean expired) {
        Date sessionAcquisitionTime = new Date();
        if (expired) {
            sessionAcquisitionTime = DateTimeUtils.addToDate(sessionAcquisitionTime, Calendar.MONTH, -1);
        }

        long sessionAcquisitionTimeLong = sessionAcquisitionTime.getTime();

        return String.format(
                FORMATTED_SESSION_COOKIE_TEMPLATE,
                String.valueOf(sessionAcquisitionTimeLong),
                String.valueOf(sessionAcquisitionTimeLong + 1));
    }

    private static CookiesContainer getCookiesContainer(List<String> responseCookies) throws Exception {
        CookiesContainer cookiesContainer = new CookiesContainer();
        for (String formattedCookieWithExpiration : responseCookies) {
            if (formattedCookieWithExpiration.startsWith("ai_user")) {
                String formattedCookie = formattedCookieWithExpiration.split("=")[1].split(";")[0];
                Cookie cookie = new Cookie(UserCookie.COOKIE_NAME, formattedCookie);

                UserCookie userCookie = new UserCookie(cookie);
                cookiesContainer.setUserCookie(userCookie);
            } else {
                String formattedCookie = formattedCookieWithExpiration.split("=")[1].split(";")[0];
                Cookie cookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

                SessionCookie sessionCookie = new SessionCookie(cookie);
                cookiesContainer.setSessionCookie(sessionCookie);
            }
        }

        return cookiesContainer;
    }
}