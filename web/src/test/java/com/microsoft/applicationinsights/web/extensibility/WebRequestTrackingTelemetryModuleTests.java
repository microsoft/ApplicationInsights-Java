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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import javax.servlet.ServletRequest;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.utils.JettyTestServer;
import com.microsoft.applicationinsights.web.utils.MockTelemetryChannel;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static com.microsoft.applicationinsights.web.utils.HttpHelper.sendRequestAndGetResponseCookie;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.util.List;

/**
 * Created by yonisha on 2/2/2015.
 */
public class WebRequestTrackingTelemetryModuleTests {
    private static JettyTestServer server = new JettyTestServer();
    private static WebRequestTrackingTelemetryModule defaultModule;
    private static MockTelemetryChannel channel;

    @BeforeClass
    public static void classInitialize() throws Exception {
        server.start();

        // Set mock channel
        channel = new MockTelemetryChannel();
        TelemetryConfiguration.getActive().setChannel(channel);
        TelemetryConfiguration.getActive().setInstrumentationKey("SOME_INT_KEY");
    }

    @Before
    public void testInitialize() {
        defaultModule = new WebRequestTrackingTelemetryModule();
        channel.reset();
    }

    @AfterClass
    public static void classCleanup() throws Exception {
        server.shutdown();
    }

    @Test
    public void testHttpRequestTrackedSuccessfully() throws Exception {
        sendRequestAndGetResponseCookie();

        List<Telemetry> items = channel.getTelemetryItems();
        assertEquals(1, items.size());
        HttpRequestTelemetry requestTelemetry = (HttpRequestTelemetry)items.get(0);

        assertEquals("200", requestTelemetry.getResponseCode());
        assertEquals("GET /", requestTelemetry.getName());
        assertEquals("GET", requestTelemetry.getHttpMethod());
        assertEquals("http://localhost:1234/", requestTelemetry.getUrl().toString());
    }

    @Test
    public void testOnBeginRequestCatchAllExceptions() {
        ServletRequest request = createFaultyServletRequestMock();

        defaultModule.onBeginRequest(request, null);
    }

    @Test
    public void testOnEndRequestCatchAllExceptions() {
        ServletRequest request = createFaultyServletRequestMock();

        defaultModule.onEndRequest(request, null);
    }

    // region Private methods

    private ServletRequest createFaultyServletRequestMock() {
        ServletRequest request = mock(ServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception("FATAL!");
            }
        }).when(request).getAttribute(any(String.class));

        return request;
    }

    // endregion Private methods
}