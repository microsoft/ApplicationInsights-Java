package com.microsoft.applicationinsights.web.internal.correlation;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Traceparent;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.Setter;

/**
 * A class that is responsible for performing correlation based on W3C protocol.
 * This is a clean implementation of W3C protocol and doesn't have the backward
 * compatibility with AI-RequestId protocol.
 *
 * @author Dhaval Doshi
 */
public class TraceContextCorrelationCore {

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
    private TraceContextCorrelationCore() {}

    /**
     * This method is responsible to perform correlation for incoming request by populating it's
     * traceId, spanId and parentId. It also stores incoming tracestate into ThreadLocal for downstream
     * propagation.
     * @param request
     * @param requestTelemetry
     */
    public static <Req> DistributedTraceContext resolveCorrelationForRequest(Req request,
                                                                             Getter<Req> requestHeaderGetter,
                                                                             RequestTelemetry requestTelemetry,
                                                                             boolean azureFnRequest) {
        try {
            if (request == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. request is null.");
                return null;
            }

            if (requestTelemetry == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. requestTelemetry is null.");
                return null;
            }

            Traceparent incomingTraceparent = extractIncomingTraceparent(request, requestHeaderGetter);
            Traceparent processedTraceParent;
            if (azureFnRequest) {
                processedTraceParent = incomingTraceparent;
            } else {
                processedTraceParent = processIncomingTraceparent(incomingTraceparent, request, requestHeaderGetter, requestTelemetry);
            }

            // represents the id of the current request.
            requestTelemetry.setId(processedTraceParent.getSpanId());

            // represents the trace-id of this distributed trace
            requestTelemetry.getContext().getOperation().setId(processedTraceParent.getTraceId());

            // assign parent id
            if (incomingTraceparent != null) {
                requestTelemetry.getContext().getOperation().setParentId(incomingTraceparent.getSpanId());
            }

            String appId = getAppId();

            // Get Tracestate header
            Tracestate tracestate = getTracestate(request, requestHeaderGetter, incomingTraceparent, appId);
            int traceflag = processedTraceParent.getTraceFlags();

            return new DistributedTraceContext(requestTelemetry, tracestate, traceflag);

        } catch (java.lang.Exception e) {
            InternalLogger.INSTANCE.error("unable to perform correlation :%s", ExceptionUtils.
                getStackTrace(e));
            return null;
        }
    }

    public static <Res> void resolveCorrelationForResponse(Res response, Setter<Res> responseHeaderSetter) {

        try {
            if (response == null) {
                InternalLogger.INSTANCE.error("Failed to resolve correlation. response is null.");
                return;
            }

            // Let the callee know the caller's AppId
            addTargetAppIdInResponseHeaderViaRequestContext(response, responseHeaderSetter);

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
    private static <Req> Traceparent extractIncomingTraceparent(Req request, Getter<Req> requestHeaderGetter) {
        Traceparent incomingTraceparent = null;

        String traceparentHeader = requestHeaderGetter.get(request, TRACEPARENT_HEADER_NAME);

        if (Strings.isNullOrEmpty(traceparentHeader)) {
            return null;
        }

        try {
            incomingTraceparent =  Traceparent.fromString(traceparentHeader);
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
    private static <Req> Traceparent processIncomingTraceparent(Traceparent incomingTraceparent,
                                                                Req request, Getter<Req> requestHeaderGetter,
                                                                RequestTelemetry requestTelemetry) {
        Traceparent processedTraceparent = null;

        // If incoming traceparent is null create a new Traceparent
        if (incomingTraceparent == null) {

            // If BackCompt mode is enabled, read the Request-Id Header
            if (isW3CBackCompatEnabled) {
                processedTraceparent = processLegacyCorrelation(request, requestHeaderGetter, requestTelemetry);
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
    private static <Req> Traceparent processLegacyCorrelation(Req request, Getter<Req> requestHeaderGetter,
                                                              RequestTelemetry requestTelemetry) {

        String requestId = requestHeaderGetter.get(request, TelemetryCorrelationUtilsCore.CORRELATION_HEADER_NAME);

        try {
            if (requestId != null && !requestId.isEmpty()) {
                String legacyOperationId = TelemetryCorrelationUtilsCore.extractRootId(requestId);
                requestTelemetry.getContext().getProperties().putIfAbsent("ai_legacyRootID", legacyOperationId);
                requestTelemetry.getContext().getOperation().setParentId(requestId);
                return new Traceparent(0, legacyOperationId, null, 0);
            }
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(String.format("unable to create traceparent from legacy request-id header"
                + " %s", ExceptionUtils.getStackTrace(e)));
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
    private static <Req> Tracestate getTracestate(Req request, Getter<Req> requestHeaderGetter,
                                                  Traceparent incomingTraceparent, String appId) {

        Tracestate tracestate= null;

        if (incomingTraceparent != null) {
            String tracestateHeader = requestHeaderGetter.get(request, TRACESTATE_HEADER_NAME);
            try {
                //create tracestate from incoming header
                tracestate = Tracestate.fromString(tracestateHeader);
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
     * This adds the Request-Context in response header so that the Callee can know what is the caller's AppId.
     * @param response HttpResponse object
     */
    private static <Res> void addTargetAppIdInResponseHeaderViaRequestContext(Res response, Setter<Res> responseHeaderSetter) {

        String appId = getAppId();
        if (appId.isEmpty()) {
            return;
        }

        // W3C protocol doesn't define any behavior for response headers.
        // This is purely AI concept and hence we use RequestContextHeader here.
        responseHeaderSetter.put(response, REQUEST_CONTEXT_HEADER_NAME,
                REQUEST_CONTEXT_HEADER_APPID_KEY + "=" + appId);
    }

    /**
     * Retrieves the appId for the current active config's instrumentation key.
     */
    public static String getAppId() {

        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey, TelemetryConfiguration.getActive());

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
    public static <Req> void resolveRequestSource(Req request, Getter<Req> requestHeaderGetter,
                                                  RequestTelemetry requestTelemetry, String instrumentationKey) {

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

            String tracestate = requestHeaderGetter.get(request, TRACESTATE_HEADER_NAME);
            if (tracestate == null || tracestate.isEmpty()) {

                if (isW3CBackCompatEnabled &&
                        requestHeaderGetter.get(request, TelemetryCorrelationUtilsCore.REQUEST_CONTEXT_HEADER_NAME) != null) {
                    InternalLogger.INSTANCE.trace("Tracestate absent, In backward compatibility mode, will try to resolve "
                        + "request-context");
                    TelemetryCorrelationUtilsCore.resolveRequestSource(request, requestHeaderGetter, requestTelemetry, instrumentationKey);
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

        String headerAppID = null;
        if (keyValue[0].equals(REQUEST_CONTEXT_HEADER_APPID_KEY)) {
            headerAppID = keyValue[1];
        }

        String currAppId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey, TelemetryConfiguration.getActive());

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
        if (StringUtils.isEmpty(instrumentationKey)) {
            throw new IllegalArgumentException("instrumentationKey should be nonnull");
        }
        if (StringUtils.isEmpty(appId)) {
            throw new IllegalArgumentException("appId should be nonnull");
        }

        String myAppId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey, TelemetryConfiguration.getActive());

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

    public static void setIsRequestIdCompatEnabled(boolean isW3CBackCompatEnabled) {
        TraceContextCorrelationCore.isW3CBackCompatEnabled = isW3CBackCompatEnabled;
        InternalLogger.INSTANCE.trace(String.format("W3C Backport mode enabled on Incoming side %s",
                isW3CBackCompatEnabled));
    }
}
