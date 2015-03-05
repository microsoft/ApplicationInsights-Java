package com.microsoft.applicationinsights.web.extensibility.initializers;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
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

    @BeforeClass
    public static void classInitialize() {
        acquisitionTime = DateTimeUtils.getDateTimeNow();
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        ThreadContext.setRequestTelemetryContext(context);

        // Set session ID for the http request.
        HttpRequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
        requestTelemetry.getContext().getUser().setId(REQUEST_USER_ID);
        requestTelemetry.getContext().getUser().setAcquisitionDate(acquisitionTime);
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
