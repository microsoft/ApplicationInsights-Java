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

package com.microsoft.applicationinsights.web.extensibility.initializers;

import org.junit.*;
import java.util.List;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.utils.JettyTestServer;
import com.microsoft.applicationinsights.web.utils.MockTelemetryChannel;

import static com.microsoft.applicationinsights.web.utils.HttpHelper.sendRequestAndGetResponseCookie;
import static org.junit.Assert.assertEquals;

/**
 * Created by yonisha on 2/17/2015.
 */
public class WebOperationNameTelemetryInitializerTests {

    private static JettyTestServer server = new JettyTestServer();
    private static MockTelemetryChannel channel;
    private static WebOperationNameTelemetryInitializer defaultInitializer = new WebOperationNameTelemetryInitializer();

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
        channel.reset();
        ThreadContext.setRequestTelemetryContext(null);
    }

    @AfterClass
    public static void classCleanup() throws Exception {
        server.shutdown();
    }

    // endregion Initialization

    // region Tests

    @Test
    public void testRequestTelemetryInitializedWithOperationName() throws Exception {
        sendRequestAndGetResponseCookie();

        List<HttpRequestTelemetry> items = channel.getTelemetryItems(HttpRequestTelemetry.class);
        assertEquals(1, items.size());
        HttpRequestTelemetry requestTelemetry = items.get(0);

        Assert.assertEquals("Operation name not match", requestTelemetry.getName(), requestTelemetry.getContext().getOperation().getName());
    }

    @Test
    public void testTelemetryInitializedWithOperationName() {
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        OperationContext operationContext = createAndInitializeTelemetry();

        Assert.assertEquals("Operation name hasn't been set.", context.getHttpRequestTelemetry().getName(), operationContext.getName());
    }

    @Test
    public void testInitializerDoesNotOverrideCustomerOperationId() {
        String customerRequestName = "CustomerRequestName";

        HttpRequestTelemetry requestTelemetry = new HttpRequestTelemetry();
        OperationContext operationContext = requestTelemetry.getContext().getOperation();
        operationContext.setName(customerRequestName);

        defaultInitializer.initialize(requestTelemetry);

        Assert.assertEquals("Customer operation name should not be changed.", customerRequestName, operationContext.getName());
    }

    @Test
    public void testOperationNameNotSetWhenRequestTelemetryContextNotInitialized() {
        OperationContext operationContext = createAndInitializeTelemetry();

        Assert.assertNull("Operation name should not be set.", operationContext.getName());
    }

    // endregion Tests

    // region Private

    private OperationContext createAndInitializeTelemetry() {
        TraceTelemetry telemetry = new TraceTelemetry();
        defaultInitializer.initialize(telemetry);

        return telemetry.getContext().getOperation();
    }

    // endregion Private
}
