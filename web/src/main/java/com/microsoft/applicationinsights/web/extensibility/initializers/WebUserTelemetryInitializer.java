package com.microsoft.applicationinsights.web.extensibility.initializers;

import java.util.Date;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

/**
 * Created by yonisha on 3/5/2015.
 */
public class WebUserTelemetryInitializer extends WebTelemetryInitializerBase {

    /**
     * Initializes the properties of the given telemetry.
     *
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to initialize.
     */
    @Override
    protected void onInitializeTelemetry(Telemetry telemetry) {
        UserContext userContext = telemetry.getContext().getUser();

        if (!Strings.isNullOrEmpty(userContext.getId())) {
            return;
        }

        HttpRequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        UserContext requestUserContext = requestTelemetry.getContext().getUser();

        userContext.setId(requestUserContext.getId());

        Date requestUserAcquisitionDate = requestUserContext.getAcquisitionDate();
        if (requestUserAcquisitionDate != null) {
            userContext.setAcquisitionDate(requestUserAcquisitionDate);
        }
    }
}
