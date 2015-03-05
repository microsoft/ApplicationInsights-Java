package com.microsoft.applicationinsights.web.extensibility.initializers;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

/**
 * Created by yonisha on 3/5/2015.
 */
public class WebSessionTelemetryInitializerTests {

    private static final String REQUEST_SESSION_ID = "Request_Session_ID";
    private WebSessionTelemetryInitializer sessionTelemetryInitializer = new WebSessionTelemetryInitializer();


    @BeforeClass
    public static void classInitialize() {
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        // Set session ID for the http request.
        HttpRequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
        requestTelemetry.getContext().getSession().setId(REQUEST_SESSION_ID);
        requestTelemetry.getContext().getSession().setIsFirst(true);
    }

    @Test
    public void testSessionIdIsUpdatedFromRequest() {
        TraceTelemetry telemetry = new TraceTelemetry();

        sessionTelemetryInitializer.onInitializeTelemetry(telemetry);

        Assert.assertEquals(REQUEST_SESSION_ID, telemetry.getContext().getSession().getId());
    }

    @Test
    public void testSessionIdNotOverriddenIfAlreadySet() {
        String expectedSessionId = "SOME_ID";
        TraceTelemetry telemetry = new TraceTelemetry();
        telemetry.getContext().getSession().setId(expectedSessionId);

        sessionTelemetryInitializer.onInitializeTelemetry(telemetry);

        Assert.assertEquals(expectedSessionId, telemetry.getContext().getSession().getId());
    }

    @Test
    public void testIsFirstSessionSetWhenRequired() {
        TraceTelemetry telemetry = new TraceTelemetry();

        sessionTelemetryInitializer.onInitializeTelemetry(telemetry);

        Assert.assertEquals("First session expected.", true, telemetry.getContext().getSession().getIsFirst());
    }
}
