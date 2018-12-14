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
     * Switch to enable W3C Backward compatibility with Legacy AI Correlation.
     * By default this is turned ON.
     */
    private static boolean isW3CBackCompatEnabled = true;

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

            Traceparent incomingTraceparent = extractIncomingTraceparent(request);
            Traceparent processedTraceParent = processIncomingTraceparent(incomingTraceparent, request);

            // represents the id of the current request.
            requestTelemetry.setId("|" + processedTraceParent.getTraceId() + "." + processedTraceParent.getSpanId()
                + ".");

            // represents the trace-id of this distributed trace
            requestTelemetry.getContext().getOperation().setId(processedTraceParent.getTraceId());

            // assign parent id
            if (incomingTraceparent != null) {
                requestTelemetry.getContext().getOperation().setParentId("|" + processedTraceParent.getTraceId() + "." +
                    incomingTraceparent.getSpanId() + ".");
            } else {
                // set parentId only if not already set (legacy processing can set it)
                if (requestTelemetry.getContext().getOperation().getParentId() == null) {
                    requestTelemetry.getContext().getOperation().setParentId(null);
                }
            }

            // Propagate trace-flags
            ThreadContext.getRequestTelemetryContext().setTraceflag(processedTraceParent.getTraceFlags());

            String appId = getAppId();

            // Get Tracestate header
            Tracestate tracestate = getTracestate(request, incomingTraceparent, appId);

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
     * Helper method to create extract Incoming Traceparent header. This method can return null.
     * @param request
     * @return Incoming Traceparent
     */
    private static Traceparent extractIncomingTraceparent(HttpServletRequest request) {
        Traceparent incomingTraceparent = null;

        Enumeration<String> traceparents = request.getHeaders(TRACEPARENT_HEADER_NAME);
        List<String> traceparentList = getEnumerationAsCollection(traceparents);

        // W3C spec mandates a request should exactly have 1 Traceparent header
        if (traceparentList.size() != 1) {
            return null;
        }

        try {
            incomingTraceparent = Traceparent.fromString(traceparentList.get(0));
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(String.format("Received invalid traceparent header with exception %s, "
                + "distributed trace might be broken", ExceptionUtils.getStackTrace(e)));
        }
        return incomingTraceparent;
    }

    /**
     * This method takes incoming traceparent object and creates a new outbound traceparent object
     * @param incomingTraceparent
     * @return
     */
    private static Traceparent processIncomingTraceparent(Traceparent incomingTraceparent,
        HttpServletRequest request) {

        Traceparent processedTraceparent = null;

        // If incoming traceparent is null create a new Traceparent
        if (incomingTraceparent == null) {

            // If BackCompt mode is enabled, read the Request-Id Header
            if (isW3CBackCompatEnabled) {
                processedTraceparent = processLegacyCorrelation(request);
            }

            if (processedTraceparent == null){
                processedTraceparent = new Traceparent();
            }

        } else {
            // create outbound traceparent inheriting traceId, flags from parent.
            processedTraceparent = new Traceparent(0, incomingTraceparent.getTraceId(), null,
                incomingTraceparent.getTraceFlags());
        }
        return processedTraceparent;
    }

    /**
     * This method processes the legacy Request-ID header for backward compatibility.
     * @param request
     * @return
     */
    private static Traceparent processLegacyCorrelation(HttpServletRequest request) {

        String requestId = request.getHeader(TelemetryCorrelationUtils.CORRELATION_HEADER_NAME);

        if (requestId != null && !requestId.isEmpty()) {
            String legacyOperationId = TelemetryCorrelationUtils.extractRootId(requestId);
            RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
            requestTelemetry.getContext().getProperties().putIfAbsent("ai_legacyRootID", legacyOperationId);
            requestTelemetry.getContext().getOperation().setParentId(requestId);
            return new Traceparent(0, legacyOperationId, null, 0);
        }
        return null;
    }

    /**
     * Helper method that extracts tracestate header from request if available and add's Azure component
     * to it. If tracestate is not available, a new tracestate with Azure component is created.
     * @param request
     * @param incomingTraceparent
     * @param appId
     * @return Tracestate
     */
    private static Tracestate getTracestate(HttpServletRequest request, Traceparent incomingTraceparent, String appId) {

        Tracestate tracestate= null;

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
        return tracestate;
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

                if (isW3CBackCompatEnabled &&
                        request.getHeader(TelemetryCorrelationUtils.REQUEST_CONTEXT_HEADER_NAME) != null) {
                    InternalLogger.INSTANCE.trace("Tracestate absent, In backward compatibility mode, will try to resolve "
                        + "request-context");
                    TelemetryCorrelationUtils.resolveRequestSource(request, requestTelemetry, instrumentationKey);
                    return;
                }
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

            //check if context is null, no incoming request is present.
            // This is likely worker role scenario, where a worker is trying
            // to create a new outbound call, so generate a new traceparent.
            if (context == null) {
               return new Traceparent().toString();
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

    public static void setIsW3CBackCompatEnabled(boolean isW3CBackCompatEnabled) {
        TraceContextCorrelation.isW3CBackCompatEnabled = isW3CBackCompatEnabled;
        InternalLogger.INSTANCE.trace(String.format("W3C Backport mode enabled on Incoming side %s",
            isW3CBackCompatEnabled));
    }
}
