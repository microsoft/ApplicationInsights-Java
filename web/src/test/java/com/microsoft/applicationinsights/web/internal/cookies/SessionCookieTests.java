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

import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.Cookie;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by yonisha on 2/5/2015.
 */
public class SessionCookieTests {

    // region Members

    private static Cookie defaultCookie;
    private static String sessionId;
    private static SessionContext sessionContext;
    private static RequestTelemetryContext requestTelemetryContextMock;

    // endregion Members

    @BeforeClass
    public static void initialize() throws Exception {
        sessionId = LocalStringsUtils.generateRandomId(true);
       
        String formattedCookie = SessionCookie.formatCookie(new String[] {
                sessionId
        });

        defaultCookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

        sessionContext = new SessionContext(new ConcurrentHashMap<String, String>());
        sessionContext.setId(sessionId);

        SessionCookie sessionCookie = new SessionCookie(defaultCookie);
        requestTelemetryContextMock = mock(RequestTelemetryContext.class);
        when(requestTelemetryContextMock.getSessionCookie()).thenReturn(sessionCookie);
    }

    // region Tests
    
    @Test
    public void testCookieParsedSuccessfully() throws Exception {
        SessionCookie sessionCookie = new SessionCookie(defaultCookie);

        Assert.assertEquals("Wrong session ID", sessionId, sessionCookie.getSessionId());
    }

    @Test
    public void testSingleCookieValue() {
        String formattedCookie = SessionCookie.formatCookie(new String[]{
                sessionId
        });

        SessionCookie sessionCookie = createSessionCookie(formattedCookie);
        
        Assert.assertEquals("Wrong session ID", sessionId, sessionCookie.getSessionId());
    }

    @Test
    public void testSessionHttpCookiePathSetForAllPages() {
        Cookie cookie = HttpCookieFactory.generateSessionHttpCookie(requestTelemetryContextMock, sessionContext, 10);

        Assert.assertEquals("Path should catch all urls", HttpCookieFactory.COOKIE_PATH_ALL_URL, cookie.getPath());
    }

    @Test
    public void testSessionHttpCookieSetMaxAge() {
        final int sessionTimeoutInMinutes = 10;

        Cookie cookie = HttpCookieFactory.generateSessionHttpCookie(requestTelemetryContextMock, sessionContext, sessionTimeoutInMinutes);

        Assert.assertEquals(sessionTimeoutInMinutes * 60, cookie.getMaxAge());
    }

    // endregion Tests

    // region Private

    private SessionCookie createSessionCookie(String cookieValue) {
        Cookie cookie = new Cookie(SessionCookie.COOKIE_NAME, cookieValue);
        return new SessionCookie(cookie);
    }

    // endregion Private
}
