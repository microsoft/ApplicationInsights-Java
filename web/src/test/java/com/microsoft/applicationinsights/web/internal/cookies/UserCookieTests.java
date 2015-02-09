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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import javax.servlet.http.Cookie;
import java.util.Date;
import java.util.UUID;

/**
 * Created by yonisha on 2/9/2015.
 */
public class UserCookieTests {
    // region Members

    private static Cookie defaultCookie;
    private static String userId;
    private static Date acquisitionTime;

    // endregion Members

    @BeforeClass
    public static void initialize() {
        userId = UUID.randomUUID().toString();
        acquisitionTime = new Date();

        String formattedCookie = UserCookie.formatCookie(new String[] {
                userId,
                String.valueOf(acquisitionTime.getTime()),
        });

        defaultCookie = new Cookie(UserCookie.COOKIE_NAME, formattedCookie);
    }

    // region Tests
    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void testCookieParsedSuccessfully() throws Exception {
        UserCookie userCookie = new UserCookie(defaultCookie);

        Assert.assertEquals("Wrong user ID", userId, userCookie.getUserId());
        Assert.assertEquals("Wrong acquisition time", acquisitionTime, userCookie.getAcquisitionDate());
    }

    @Test
    public void testCorruptedUserIdValueThrowsExceptionOnCookieParsing() throws Exception {
        thrown.expect(Exception.class);

        String formattedCookie = UserCookie.formatCookie(new String[] {
                "non-UUID-string",
                String.valueOf(acquisitionTime.getTime())
        });

        createUserCookie(formattedCookie);
    }

    @Test
    public void testCorruptedAcquisitionDateValueThrowsExceptionOnCookieParsing() throws Exception {
        thrown.expect(Exception.class);

        String formattedCookie = UserCookie.formatCookie(new String[] {
                userId,
                "corruptedAcquisitionTime"
        });

        createUserCookie(formattedCookie);
    }

    // endregion Tests

    // region Private

    private void createUserCookie(String cookieValue) throws Exception {
        Cookie corruptedCookie = new Cookie(UserCookie.COOKIE_NAME, cookieValue);
        new UserCookie(corruptedCookie);
    }

    // endregion Private
}
