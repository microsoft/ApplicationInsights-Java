package com.microsoft.applicationinsights.web.internal.correlation;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A class that is responsible for performing correlation based on W3C protocol.
 * This is a clean implementation of W3C protocol and doesn't have the backward
 * compatibility with AI-RequestId protocol.
 *
 * @author Dhaval Doshi
 */
public class TraceContextCorrelation {

    public static final String TRACEPARENT_HEADER_NAME = "traceparent";
    public static final String TRACESTATE_HEADER_NAME = "tracestate";
    public static final String REQUEST_CONTEXT_HEADER_NAME = "Request-Context";
    public static final String AZURE_TRACEPARENT_COMPONENT_INITIAL = "az";
    public static final String REQUEST_CONTEXT_HEADER_APPID_KEY = "appId";

    /**
     * Private constructor as we don't expect to create an object of this class.
     */
    private TraceContextCorrelation() {}

    /**
     * This method is responsible to perform correlation for incoming request by populating it's
     * traceId, spanId and parentId. It also stores incoming tracestate into ThreadLocal for downstream
     * propagation.
     * @param request
     * @param response
     * @param requestTelemetry
     */
    public static void resolveCorrelation(HttpServletRequest request, HttpServletResponse response,
                                          RequestTelemetry requestTelemetry) {
        TraceContextCorrelationCore.resolveCorrelationForRequest(request,
                TelemetryCorrelationUtils.REQUEST_HEADER_GETTER, requestTelemetry);
        TraceContextCorrelationCore.resolveCorrelationForResponse(response,
                TelemetryCorrelationUtils.RESPONSE_HEADER_SETTER);
    }

    /**
     * Retrieves the appId for the current active config's instrumentation key.
     */
    public static String getAppId() {
        return TraceContextCorrelationCore.getAppId();
    }

    /**
     * Resolves the source of a request based on request header information and the appId of the current
     * component, which is retrieved via a query to the AppInsights service.
     * @param request The servlet request.
     * @param requestTelemetry The request telemetry in which source will be populated.
     * @param instrumentationKey The instrumentation key for the current component.
     */
    public static void resolveRequestSource(HttpServletRequest request, RequestTelemetry requestTelemetry, String instrumentationKey) {
        TraceContextCorrelationCore.resolveRequestSource(request, TelemetryCorrelationUtils.REQUEST_HEADER_GETTER,
                requestTelemetry, instrumentationKey);
    }


    /**
     * Generates the target appId to add to Outbound call
     * @param requestContext
     * @return
     */
    public static String generateChildDependencyTarget(String requestContext) {
        return TraceContextCorrelationCore.generateChildDependencyTarget(requestContext);
    }

    /**
     * Helper method to retrieve Tracestate from ThreadLocal
     * @return
     */
    public static String retriveTracestate() {
        return TraceContextCorrelationCore.retriveTracestate();
    }

    /**
     * Generates child TraceParent by retrieving values from ThreadLocal.
     * @return Outbound Traceparent
     */
    public static String generateChildDependencyTraceparent() {
        return TraceContextCorrelationCore.generateChildDependencyTraceparent();
    }

    /**
     * This is helper method to convert traceparent (W3C) format to AI legacy format for supportability
     * @param traceparent
     * @return legacy format traceparent
     */
    public static String createChildIdFromTraceparentString(String traceparent) {
        return TraceContextCorrelationCore.createChildIdFromTraceparentString(traceparent);
    }

    public static void setIsW3CBackCompatEnabled(boolean isW3CBackCompatEnabled) {
        TraceContextCorrelationCore.setIsW3CBackCompatEnabled(isW3CBackCompatEnabled);
    }
}
