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

package com.microsoft.applicationinsights.web.extensibility.initializers;

import org.junit.*;
import java.util.List;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
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
public class WebOperationIdTelemetryInitializerTests {

    private static JettyTestServer server = new JettyTestServer();
    private static MockTelemetryChannel channel;
    private static WebOperationIdTelemetryInitializer defaultInitializer = new WebOperationIdTelemetryInitializer();

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
    public void testRequestTelemetryInitializedWithOperationId() throws Exception {
        sendRequestAndGetResponseCookie(server.getPortNumber());

        List<RequestTelemetry> items = channel.getTelemetryItems(RequestTelemetry.class);
        assertEquals(1, items.size());
        RequestTelemetry requestTelemetry = items.get(0);

        // the WebRequestTrackingModule automatically creates a hierarchical ID for request telemetry of the 
        // following form: "|guid.", where guid is the OperationId
        Assert.assertEquals("Operation id not match", requestTelemetry.getId(), "|" + requestTelemetry.getContext().getOperation().getId() + ".");
    }

    @Test
    public void testTelemetryInitializedWithOperationId() {
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        OperationContext operationContext = createAndInitializeTelemetry();

        Assert.assertEquals("Operation ID hasn't been set.", context.getHttpRequestTelemetry().getId(), operationContext.getId());
    }

    @Test
    public void testInitializerDoesNotOverrideCustomerOperationId() {
        String customerId = "CustomerID";

        RequestTelemetry requestTelemetry = new RequestTelemetry();
        OperationContext operationContext = requestTelemetry.getContext().getOperation();
        operationContext.setId(customerId);

        defaultInitializer.initialize(requestTelemetry);

        Assert.assertEquals("Customer operation ID should not be changed.", customerId, operationContext.getId());
    }

    @Test
    public void testOperationIdNotSetWhenRequestTelemetryContextNotInitialized() {
        OperationContext operationContext = createAndInitializeTelemetry();

        Assert.assertNull("Operation ID should not be set.", operationContext.getId());
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
