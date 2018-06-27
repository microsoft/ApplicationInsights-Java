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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import java.util.Date;
import javax.servlet.http.Cookie;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Created by yonisha on 2/9/2015. */
public class UserCookieTests {
  // region Members

  private static Cookie defaultCookie;
  private static String userId;
  private static Date acquisitionTime;
  private static RequestTelemetryContext requestTelemetryContextMock;

  // endregion Members
  // region Tests
  @Rule public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void initialize() throws Exception {
    userId = LocalStringsUtils.generateRandomId(true);
    acquisitionTime = new Date();

    String formattedCookie =
        UserCookie.formatCookie(
            new String[] {
              userId, DateTimeUtils.formatAsRoundTripDate(acquisitionTime),
            });

    defaultCookie = new Cookie(UserCookie.COOKIE_NAME, formattedCookie);

    UserCookie userCookie = new UserCookie(defaultCookie);
    requestTelemetryContextMock = mock(RequestTelemetryContext.class);
    when(requestTelemetryContextMock.getUserCookie()).thenReturn(userCookie);
  }

  @Test
  public void testCookieParsedSuccessfully() throws Exception {
    UserCookie userCookie = new UserCookie(defaultCookie);

    Date expectedAcquisitionTime =
        DateTimeUtils.parseRoundTripDateString(
            DateTimeUtils.formatAsRoundTripDate(acquisitionTime));
    Assert.assertEquals("Wrong user ID", userId, userCookie.getUserId());
    Assert.assertEquals(
        "Wrong acquisition time", expectedAcquisitionTime, userCookie.getAcquisitionDate());
  }

  @Test
  public void testCorruptedAcquisitionDateValueThrowsExceptionOnCookieParsing() throws Exception {
    thrown.expect(Exception.class);

    String formattedCookie =
        UserCookie.formatCookie(new String[] {userId, "corruptedAcquisitionTime"});

    createUserCookie(formattedCookie);
  }

  @Test
  public void testUnexpectedCookieValuesCountThrowsException() throws Exception {
    thrown.expect(Exception.class);

    String formattedCookie = SessionCookie.formatCookie(new String[] {"singleValueCookie"});

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
