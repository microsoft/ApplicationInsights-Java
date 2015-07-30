package com.microsoft.applicationinsights.web.extensibility.initializers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

import java.util.Date;

/**
 * Created by yonisha on 3/5/2015.
 */
public class WebUserTelemetryInitializerTests {

    private static final String REQUEST_USER_ID = "Request_USER_ID";
    private static Date acquisitionTime;
    private WebUserTelemetryInitializer userTelemetryInitializer = new WebUserTelemetryInitializer();

    @Before
    public void classInitialize() {
        acquisitionTime = DateTimeUtils.getDateTimeNow();
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        // Set session ID for the http request.
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
        requestTelemetry.getContext().getUser().setId(REQUEST_USER_ID);
        requestTelemetry.getContext().getUser().setAcquisitionDate(acquisitionTime);
    }

    @Test
    public void testNoUserCookie() {
        RequestTelemetryContext requestTelemetryContext = ThreadContext.getRequestTelemetryContext();
        requestTelemetryContext.getHttpRequestTelemetry().getContext().getUser().setAcquisitionDate(null);
        requestTelemetryContext.getHttpRequestTelemetry().getContext().getUser().setId(null);

        TraceTelemetry telemetry = new TraceTelemetry();

        userTelemetryInitializer.onInitializeTelemetry(telemetry);

        Assert.assertNull(telemetry.getContext().getUser().getId());
        Assert.assertNull(telemetry.getContext().getUser().getAcquisitionDate());
    }

    @Test
    public void testUserIdIsUpdatedFromRequest() {
        TraceTelemetry telemetry = new TraceTelemetry();

        userTelemetryInitializer.onInitializeTelemetry(telemetry);

        Assert.assertEquals(REQUEST_USER_ID, telemetry.getContext().getUser().getId());
    }

    @Test
    public void testUserIdNotOverriddenIfAlreadySet() {
        String expectedUserId = "SOME_ID";
        TraceTelemetry telemetry = new TraceTelemetry();
        telemetry.getContext().getUser().setId(expectedUserId);

        userTelemetryInitializer.onInitializeTelemetry(telemetry);

        Assert.assertEquals(expectedUserId, telemetry.getContext().getUser().getId());
    }

    @Test
    public void testAcquisitionDateSetWhenRequired() {
        TraceTelemetry telemetry = new TraceTelemetry();

        userTelemetryInitializer.onInitializeTelemetry(telemetry);

        Assert.assertEquals(acquisitionTime, telemetry.getContext().getUser().getAcquisitionDate());
    }
}
