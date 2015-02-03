package com.microsoft.applicationinsights.web.extensibility;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;

/**
 * Created by yonisha on 2/2/2015.
 */
public class WebRequestTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule {
    private TelemetryClient telemetryClient;

    /**
     * Begin request processing.
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onBeginRequest(ServletRequest req, ServletResponse res) {
        try {
            RequestTelemetryContext context = getTelemetryContextFromServletRequest(req);
            HttpRequestTelemetry telemetry = context.getHttpRequestTelemetry();

            HttpServletRequest request = (HttpServletRequest) req;
            String method = request.getMethod();
            String rURI = request.getRequestURI();
            String scheme = request.getScheme();
            String host = request.getHeader("Host");
            String query = request.getQueryString();


            telemetry.setHttpMethod(method);
            if (query != null && query.length() > 0) {
                telemetry.setUrl(String.format("%s://%s%s?%s", scheme, host, rURI, query));
            }
            else {
                telemetry.setUrl(String.format("%s://%s%s", scheme, host, rURI));
            }

            telemetry.setName(String.format("%s %s", method, rURI));
            telemetry.setTimestamp(new Date(context.getRequestStartTimeTicks()));
        } catch (Exception e) {
            String moduleClassName = this.getClass().getSimpleName();

            InternalLogger.INSTANCE.log("Telemetry module " + moduleClassName + " onBeginRequest failed with exception: %s", e.getMessage());
        }
    }

    /**
     * End request processing.
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onEndRequest(ServletRequest req, ServletResponse res) {
        try {
            RequestTelemetryContext context = getTelemetryContextFromServletRequest(req);
            HttpRequestTelemetry telemetry = context.getHttpRequestTelemetry();

            long endTime = new Date().getTime();

            HttpServletResponse response = (HttpServletResponse)res;
            telemetry.setSuccess(200 == response.getStatus());
            telemetry.setResponseCode(Integer.toString(response.getStatus()));
            telemetry.setDuration(endTime - context.getRequestStartTimeTicks());

            telemetryClient.track(telemetry);
        } catch (Exception e) {
            String moduleClassName = this.getClass().getSimpleName();

            InternalLogger.INSTANCE.log("Telemetry module " + moduleClassName + " onEndRequest failed with exception: %s", e.getMessage());
        }
    }

    /**
     * Initializes the telemetry module with the given telemetry configuration.
     * @param configuration The telemetry configuration.
     */
    @Override
    public void initialize(TelemetryConfiguration configuration) {
        telemetryClient = new TelemetryClient(configuration);
    }

    // region Private methods

    private RequestTelemetryContext getTelemetryContextFromServletRequest(ServletRequest request) {
        return (RequestTelemetryContext)request.getAttribute(RequestTelemetryContext.CONTEXT_ATTR_KEY);
    }

    // endregion Private methods
}
