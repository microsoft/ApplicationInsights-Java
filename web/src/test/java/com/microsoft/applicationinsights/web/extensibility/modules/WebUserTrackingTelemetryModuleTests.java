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

package com.microsoft.applicationinsights.web.extensibility.modules;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.utils.MockTelemetryChannel;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import com.microsoft.applicationinsights.web.utils.CookiesContainer;
import com.microsoft.applicationinsights.web.utils.HttpHelper;
import com.microsoft.applicationinsights.web.utils.JettyTestServer;

/**
 * Created by yonisha on 2/9/2015.
 */
public class WebUserTrackingTelemetryModuleTests {
    private static class TestModuleInitializer implements TelemetryInitializer {
        private final String expectedUserId;

        private TestModuleInitializer(String expectedUserId) {
            this.expectedUserId = expectedUserId;
        }

        @Override
        public void initialize(Telemetry telemetry) {
            RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
            UserContext requestUserContext = requestTelemetry.getContext().getUser();

            if (expectedUserId == null) {
                //No longer testing for UUID
                //Assert.assertTrue(Sanitizer.isUUID(requestUserContext.getId()));
                Assert.assertNotEquals(requestUserContext.getId(), HttpHelper.getCookie());
            } else {
                Assert.assertEquals(expectedUserId, requestUserContext.getId());
            }
            Assert.assertNotNull(requestUserContext.getAcquisitionDate());
        }
    }

    // region Members

    private static String userCookieFormatted;
    private static JettyTestServer server = new JettyTestServer();
    private static MockTelemetryChannel channel;

    // endregion Members

    // region Initialization

    @BeforeClass
    public static void classInitialize() throws Exception {
        server.start();

        // Set mock channel
        channel = MockTelemetryChannel.INSTANCE;
        TelemetryConfiguration.getActive().setChannel(channel);
        TelemetryConfiguration.getActive().setInstrumentationKey("SOME_INT_KEY");
    }

    @Before
    public void testInitialize() {
        userCookieFormatted = HttpHelper.getFormattedUserCookieHeader();
        channel.reset();
    }

    @AfterClass
    public static void classCleanup() throws Exception {
        server.shutdown();
    }

    // endregion Initialization

    // region Tests

    @Test
    public void testNewUserCookieIsNotCreatedWhenCookieNotExist() throws Exception {
        TelemetryConfiguration.getActive().getTelemetryInitializers().add(new TestModuleInitializer(null));

        CookiesContainer cookiesContainer = HttpHelper.sendRequestAndGetResponseCookie(server.getPortNumber());

        Assert.assertNull("User cookie should be null.", cookiesContainer.getUserCookie());
    }

    @Test
    public void testWhenCookieExistCorrectUserIdAttachedToSentTelemetry() throws Exception {
        HttpHelper.sendRequestAndGetResponseCookie(server.getPortNumber(), userCookieFormatted);

        RequestTelemetry requestTelemetry = channel.getTelemetryItems(RequestTelemetry.class).get(0);

        Assert.assertTrue(userCookieFormatted.contains(requestTelemetry.getContext().getUser().getId()));
        Assert.assertEquals(requestTelemetry.getContext().getUser().getId(), HttpHelper.getCookie());
    }

    @Test
    public void testNoUserCookieCreatedWhenValidCookieExists() throws Exception {
        CookiesContainer cookiesContainer = HttpHelper.sendRequestAndGetResponseCookie(server.getPortNumber(),userCookieFormatted);

        Assert.assertNull(cookiesContainer.getUserCookie());
    }

    // endregion Tests
}
