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

package com.microsoft.applicationinsights.web.extensibility;

import javax.servlet.http.Cookie;
import org.junit.*;
import com.microsoft.applicationinsights.web.utils.HttpHelper;
import com.microsoft.applicationinsights.internal.util.Sanitizer;
import com.microsoft.applicationinsights.web.internal.cookies.SessionCookie;
import com.microsoft.applicationinsights.web.utils.JettyTestServer;

/**
 * Created by yonisha on 2/5/2015.
 */
public class WebSessionTrackingTelemetryModuleTests {

    // region Consts

    private static String sessionCookieFormatted;

    // endregion Consts

    // region Members

    private static JettyTestServer server = new JettyTestServer();

    // endregion Members

    // region Initialization

    @BeforeClass
    public static void classInitialize() throws Exception {
        server.start();
    }

    @Before
    public void testInitialize() {
        sessionCookieFormatted = HttpHelper.getFormattedSessionCookieHeader(false);
    }

    @AfterClass
    public static void classCleanup() throws Exception {
        server.shutdown();
    }

    // endregion Initialization

    // region Tests

    @Test
    public void testNewSessionIsCreatedWhenCookieNotExist() throws Exception {
        String formattedResponseCookie = HttpHelper.sendRequestAndGetResponseCookie();

        SessionCookie cookie = new SessionCookie(new Cookie(SessionCookie.SESSION_COOKIE_NAME, formattedResponseCookie));

        Assert.assertTrue(Sanitizer.isUUID(cookie.getSessionId()));
    }

    @Test
    public void testNoSessionCreatedWhenValidSessionExists() throws Exception {
        String formattedResponseCookie = HttpHelper.sendRequestAndGetResponseCookie(sessionCookieFormatted);

        Assert.assertNull(formattedResponseCookie);
    }

    @Test
    public void testNewSessionIsCreatedWhenCookieSessionExpired() throws Exception {
        sessionCookieFormatted = HttpHelper.getFormattedSessionCookieHeader(true);

        String formattedResponseCookie = HttpHelper.sendRequestAndGetResponseCookie(sessionCookieFormatted);

        Assert.assertNotNull(formattedResponseCookie);
        Assert.assertTrue(!formattedResponseCookie.equals(sessionCookieFormatted));
    }

    // endregion Tests
}
