package com.microsoft.applicationinsights.web.extensibility.initializers;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;

/**
 * Created by yonisha on 3/5/2015.
 */
public class WebSessionTelemetryInitializer extends WebTelemetryInitializerBase {

    /**
     * Initializes the properties of the given telemetry.
     *
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} to initialize.
     */
    @Override
    protected void onInitializeTelemetry(Telemetry telemetry) {
        SessionContext session = telemetry.getContext().getSession();

        if (!Strings.isNullOrEmpty(session.getId())) {
            return;
        }

        HttpRequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        SessionContext requestSessionContext = requestTelemetry.getContext().getSession();

        session.setId(requestSessionContext.getId());

        Boolean isFirstSession = requestSessionContext.getIsFirst();
        if (isFirstSession != null && isFirstSession) {
            session.setIsFirst(isFirstSession);
        }
    }
}
