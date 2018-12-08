package com.microsoft.applicationinsights.web.internal.correlation;

import com.google.common.base.Joiner;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Traceparent;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;

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

        try {
            if (request == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. request is null.");
                return;
            }

            if (response == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. response is null.");
                return;
            }

            if (requestTelemetry == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. requestTelemetry is null.");
                return;
            }

            // According to W3C spec there can be more than 1 traceparents in incoming request
            Enumeration<String> traceparents = request.getHeaders(TRACEPARENT_HEADER_NAME);
            List<String> traceparentList = getEnumerationAsCollection(traceparents);

            Traceparent incomingTraceparent = null;
            Traceparent outGoingTraceParent = null;

            // If no incoming traceparent or multiple traceparent create a  new one.
            if (traceparentList.size() != 1) {
                outGoingTraceParent = new Traceparent();

                // represents the id of the current request.
                requestTelemetry.setId("|" + outGoingTraceParent.getTraceId() + "." + outGoingTraceParent.getSpanId()
                + ".");

                // represents the trace-id of this distributed trace
                requestTelemetry.getContext().getOperation().setId(outGoingTraceParent.getTraceId());

                // set parentId as null because this is the is the originating request
                requestTelemetry.getContext().getOperation().setParentId(null);
            } else {

                try {
                    incomingTraceparent = Traceparent.fromString(traceparentList.get(0));

                    // create outgoing traceParent using the incoming header
                    // correct names
                    outGoingTraceParent = new Traceparent(0, incomingTraceparent.getTraceId(),
                        null, incomingTraceparent.getTraceFlags());

                } catch (IllegalArgumentException e) {
                    InternalLogger.INSTANCE.error(String.format("Received invalid traceparent header with exception %s, "
                        + "distributed trace might be broken", ExceptionUtils.getStackTrace(e)));
                } finally {
                    if (incomingTraceparent == null) {

                        // Invalid incoming traceparent. Create a new outgoing traceparent
                        outGoingTraceParent = new Traceparent();
                    }
                    // set id of this request
                    requestTelemetry.setId("|" + outGoingTraceParent.getTraceId() + "." + outGoingTraceParent.getSpanId()
                    + ".");

                    // represents the trace-id of this distributed trace
                    requestTelemetry.getContext().getOperation().setId(outGoingTraceParent.getTraceId());

                    if (incomingTraceparent != null) {
                        // represents the parent-id of this request which is combination of traceparent and incoming spanId
                        requestTelemetry.getContext().getOperation().setParentId("|" + outGoingTraceParent.getTraceId() + "." +
                            incomingTraceparent.getSpanId() + ".");
                    } else {
                        requestTelemetry.getContext().getOperation().setParentId(null);
                    }

                }

            }

            // Propagate trace-flags
            ThreadContext.getRequestTelemetryContext().setTraceflag(outGoingTraceParent.getTraceFlags());

            String appId = getAppId();

            // Get Tracestate header
            Tracestate tracestate = null;

            if (incomingTraceparent != null) {
                Enumeration<String> tracestates = request.getHeaders(TRACESTATE_HEADER_NAME);
                List<String> tracestateList = getEnumerationAsCollection(tracestates);
                try {
                    //create tracestate from incoming header
                    tracestate = Tracestate.fromString(Joiner.on(",").join(tracestateList));
                    // add appId to it if it's resolved
                    if (appId != null && !appId.isEmpty()) {
                        tracestate = new Tracestate(tracestate, AZURE_TRACEPARENT_COMPONENT_INITIAL,
                            appId);
                    }

                } catch (Exception e) {
                    InternalLogger.INSTANCE.error(String.format("Cannot parse incoming tracestate %s",
                        ExceptionUtils.getStackTrace(e)));
                    try {
                        // Pass new tracestate if received invalid tracestate
                        if (appId != null && !appId.isEmpty()) {
                            tracestate = new Tracestate(null, AZURE_TRACEPARENT_COMPONENT_INITIAL, appId);
                        }
                    } catch (Exception ex) {
                        InternalLogger.INSTANCE.error(String.format("Cannot create default tracestate %s",
                            ExceptionUtils.getStackTrace(ex)));
                    }
                }
            } else {
                // pass new tracestate if incoming traceparent is empty
                try {
                    if (appId != null && !appId.isEmpty()) {
                        tracestate = new Tracestate(null, AZURE_TRACEPARENT_COMPONENT_INITIAL, appId);
                    }
                } catch (Exception e) {
                    InternalLogger.INSTANCE.error(String.format("cannot create default traceparent %s",
                        ExceptionUtils.getStackTrace(e)));
                }
            }

            // add tracestate to threadlocal
            ThreadContext.getRequestTelemetryContext().setTracestate(tracestate);

            // Let the callee know the caller's AppId
            addTargetAppIdInResponseHeaderViaRequestContext(response);

        } catch (java.lang.Exception e) {
            InternalLogger.INSTANCE.error("unable to perform correlation :%s", ExceptionUtils.
                getStackTrace(e));
        }
    }

    /**
     * Returns collection from Enumeration
     * @param e
     * @return List of headers
     */
    private static List<String> getEnumerationAsCollection(Enumeration<String> e) {

        List<String> list = new ArrayList<>();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        return list;
    }

    /**
     * This adds the Request-Context in response header so that the Callee can know what is the caller's AppId.
     * @param response HttpResponse object
     */
    private static void addTargetAppIdInResponseHeaderViaRequestContext(HttpServletResponse response) {

        if (response.containsHeader(REQUEST_CONTEXT_HEADER_NAME)) {
            return;
        }

        String appId = getAppIdWithKey();
        if (appId.isEmpty()) {
            return;
        }

        // W3C protocol doesn't define any behavior for response headers.
        // This is purely AI concept and hence we use RequestContextHeader here.
        response.addHeader(REQUEST_CONTEXT_HEADER_NAME,appId);
    }

    /**
     * Gets AppId prefixed with key to append to Request-Context header
     * @return
     */
    private static String getAppIdWithKey() {
        return REQUEST_CONTEXT_HEADER_APPID_KEY + "=" + getAppId();
    }

    /**
     * Retrieves the appId for the current active config's instrumentation key.
     */
    public static String getAppId() {

        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey);

        //it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case, just
        //return and let the next request resolve the ikey.
        if (appId == null) {
            InternalLogger.INSTANCE.trace("Application correlation Id could not be retrieved (e.g. task may be pending or failed)");
            return "";
        }

        return appId;
    }

    /**
     * Resolves the source of a request based on request header information and the appId of the current
     * component, which is retrieved via a query to the AppInsights service.
     * @param request The servlet request.
     * @param requestTelemetry The request telemetry in which source will be populated.
     * @param instrumentationKey The instrumentation key for the current component.
     */
    public static void resolveRequestSource(HttpServletRequest request, RequestTelemetry requestTelemetry, String instrumentationKey) {

        try {

            if (request == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. request is null.");
                return;
            }

            if (instrumentationKey == null || instrumentationKey.isEmpty()) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. InstrumentationKey is null or empty.");
                return;
            }

            if (requestTelemetry == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. requestTelemetry is null.");
                return;
            }

            if (requestTelemetry.getSource() != null) {
                InternalLogger.INSTANCE.trace("Skip resolving request source as it is already initialized.");
                return;
            }

            String tracestate = request.getHeader(TRACESTATE_HEADER_NAME);
            if (tracestate == null || tracestate.isEmpty()) {
                InternalLogger.INSTANCE.info("Skip resolving request source as the following header was not found: %s",
                    TRACESTATE_HEADER_NAME);
                return;
            }

            Tracestate incomingTracestate = Tracestate.fromString(tracestate);

            String source = generateSourceTargetCorrelation(instrumentationKey,
                incomingTracestate.get(AZURE_TRACEPARENT_COMPONENT_INITIAL));

            // Set the source of this request telemetry which would be equal to AppId of the caller if
            // it's different from current AppId or else null.
            requestTelemetry.setSource(source);

        }
        catch(Exception ex) {
            InternalLogger.INSTANCE.error("Failed to resolve request source. Exception information: %s",
                ExceptionUtils.getStackTrace(ex));
        }
    }


    /**
     * Generates the target appId to add to Outbound call
     * @param requestContext
     * @return
     */
    public static String generateChildDependencyTarget(String requestContext) {
        if (requestContext == null || requestContext.isEmpty()) {
            InternalLogger.INSTANCE.trace("generateChildDependencyTarget: won't continue as requestContext is null or empty.");
            return "";
        }

        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        if (instrumentationKey == null || instrumentationKey.isEmpty()) {
            InternalLogger.INSTANCE.error("Failed to generate target correlation. InstrumentationKey is null or empty.");
            return "";
        }

        // In W3C we only pass requestContext for the response. So it's expected to have only single key-value pair
        String[] keyValue = requestContext.split("=");
        assert keyValue.length == 2;

        String headerAppID = null;
        if (keyValue[0].equals(REQUEST_CONTEXT_HEADER_APPID_KEY)) {
            headerAppID = keyValue[1];
        }

        String currAppId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(TelemetryConfiguration.getActive()
        .getInstrumentationKey());

        String target = resolve(headerAppID, currAppId);
        if (target == null) {
            InternalLogger.INSTANCE.warn("Target value is null and hence returning empty string");
            return ""; // we want an empty string instead of null so it plays nicer with bytecode injection
        }
        return target;
    }

    /**
     * Extracts the appId/roleName out of Tracestate and compares it with the current appId. It then
     * generates the appropriate source or target.
     */
    private static String generateSourceTargetCorrelation(String instrumentationKey, String appId) {

        assert instrumentationKey != null;
        assert appId != null;

        String myAppId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey);

        return resolve(appId, myAppId);
    }

    /**
     * Resolves appId based on appId passed in header and current appId
     * @param headerAppId
     * @param currentAppId
     * @return
     */
    private static String resolve(String headerAppId, String currentAppId) {

        //it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case, just
        //return and let the next request resolve the ikey.
        if (currentAppId == null) {
            InternalLogger.INSTANCE.trace("Could not generate source/target correlation as the appId could not be resolved (e.g. task may be pending or failed)");
            return null;
        }

        // if the current appId and the incoming appId are send null
        String result = null;
        if (headerAppId != null && !headerAppId.equals(currentAppId)) {
            result = headerAppId;
        }

        return result;
    }

    /**
     * Helper method to retrieve Tracestate from ThreadLocal
     * @return
     */
    public static String retriveTracestate() {
        //check if context is null - no correlation will happen
        if (ThreadContext.getRequestTelemetryContext() == null || ThreadContext.getRequestTelemetryContext().
                getTracestate() == null) {
            InternalLogger.INSTANCE.warn("No correlation wil happen, Thread context is null");
            return null;
        }

        Tracestate tracestate = ThreadContext.getRequestTelemetryContext().getTracestate();
        return tracestate.toString();
    }

    /**
     * Generates child TraceParent by retrieving values from ThreadLocal.
     * @return Outbound Traceparent
     */
    public static String generateChildDependencyTraceparent() {
        try {

            RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

            //check if context is null - no correlation will happen
            if (context == null) {
                InternalLogger.INSTANCE.warn("No Correlation will happen, Thread context is null while generating child dependency");
                return "";
            }

            RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
            String traceId = requestTelemetry.getContext().getOperation().getId();

            Traceparent tp = new Traceparent(0, traceId, null, context.getTraceflag());

            // We need to propagate full blown traceparent header.
            return tp.toString();
        }
        catch (Exception ex) {
            InternalLogger.INSTANCE.error("Failed to generate child ID. Exception information: %s", ex.toString());
            InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(ex));
        }

        return null;
    }

    /**
     * This is helper method to convert traceparent (W3C) format to AI legacy format for supportability
     * @param traceparent
     * @return legacy format traceparent
     */
    public static String createChildIdFromTraceparentString(String traceparent) {
        assert traceparent != null;

        String[] traceparentArr = traceparent.split("-");
        assert traceparentArr.length == 4;

        return "|" + traceparentArr[1] + "." + traceparentArr[2] + ".";
    }
}
