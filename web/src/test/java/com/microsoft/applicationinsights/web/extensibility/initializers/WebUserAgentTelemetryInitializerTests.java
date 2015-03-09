package com.microsoft.applicationinsights.web.extensibility.initializers;

import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.utils.HttpHelper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by yonisha on 3/9/2015.
 */
public class WebUserAgentTelemetryInitializerTests {

    private WebUserAgentTelemetryInitializer initializer = new WebUserAgentTelemetryInitializer();

    @BeforeClass
    public static void classInitialize() {
        RequestTelemetryContext context = new RequestTelemetryContext(DateTimeUtils.getDateTimeNow().getTime());
        context.getHttpRequestTelemetry().getContext().getUser().setUserAgent(HttpHelper.TEST_USER_AGENT);
        ThreadContext.setRequestTelemetryContext(context);
    }

    @Test
    public void testTelemetryIsUpdatedWithUserAgent() {
        TraceTelemetry telemetry = new TraceTelemetry("new telemetry");

        initializer.onInitializeTelemetry(telemetry);

        Assert.assertEquals(HttpHelper.TEST_USER_AGENT, telemetry.getContext().getUser().getUserAgent());
    }
}
