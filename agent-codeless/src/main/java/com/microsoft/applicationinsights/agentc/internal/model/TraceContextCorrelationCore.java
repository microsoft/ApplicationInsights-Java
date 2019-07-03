package com.microsoft.applicationinsights.agentc.internal.model;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.InstrumentationKeyResolver;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Traceparent;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that is responsible for performing correlation based on W3C protocol.
 * This is a clean implementation of W3C protocol and doesn't have the backward
 * compatibility with AI-RequestId protocol.
 *
 * @author Dhaval Doshi
 */
public class TraceContextCorrelationCore {

    private static final Logger logger = LoggerFactory.getLogger(TraceContextCorrelationCore.class);

    private static final String TRACEPARENT_HEADER_NAME = "traceparent";
    private static final String TRACESTATE_HEADER_NAME = "tracestate";
    private static final String REQUEST_CONTEXT_HEADER_NAME = "Request-Context";
    private static final String AZURE_TRACEPARENT_COMPONENT_INITIAL = "az";
    private static final String REQUEST_CONTEXT_HEADER_APPID_KEY = "appId";

    /**
     * Switch to enable W3C Backward compatibility with Legacy AI Correlation.
     * By default this is turned ON.
     */
    private static boolean isW3CBackCompatEnabled = true;

    private TraceContextCorrelationCore() {
    }

    public static <Req> DistributedTraceContext resolveCorrelationForRequest(Req request,
                                                                             Getter<Req> requestHeaderGetter,
                                                                             RequestTelemetry requestTelemetry) {
        try {
            Traceparent incomingTraceparent = extractIncomingTraceparent(request, requestHeaderGetter);
            Traceparent processedTraceParent =
                    processIncomingTraceparent(incomingTraceparent, request, requestHeaderGetter, requestTelemetry);

            // id of the current request
            requestTelemetry
                    .setId("|" + processedTraceParent.getTraceId() + "." + processedTraceParent.getSpanId() + ".");

            // trace-id of the distributed trace
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

            Tracestate tracestate = getTracestate(request, requestHeaderGetter, incomingTraceparent, getAppId());
            int traceflag = processedTraceParent.getTraceFlags();

            return new DistributedTraceContext(requestTelemetry, tracestate, traceflag);

        } catch (Exception e) {
            logger.debug("unable to perform correlation", e);
            return null;
        }
    }

    public static <Res> void resolveCorrelationForResponse(Res response, Setter<Res> responseHeaderSetter) {

        try {
            // Let the callee know the caller's AppId
            addTargetAppIdInResponseHeaderViaRequestContext(response, responseHeaderSetter);

        } catch (Exception e) {
            logger.debug("unable to perform correlation", e);
        }
    }

    /**
     * Helper method to create extract Incoming Traceparent header. This method can return null.
     *
     * @param request
     * @return Incoming Traceparent
     */
    @Nullable
    private static <Req> Traceparent extractIncomingTraceparent(Req request, Getter<Req> requestHeaderGetter) {

        String traceparentHeader = requestHeaderGetter.get(request, TRACEPARENT_HEADER_NAME);

        if (Strings.isNullOrEmpty(traceparentHeader)) {
            return null;
        }

        try {
            return Traceparent.fromString(traceparentHeader);
        } catch (Exception e) {
            logger.debug("Received invalid traceparent header, distributed trace might be broken", e);
            return null;
        }
    }

    /**
     * This method takes incoming traceparent object and creates a new outbound traceparent object
     *
     * @param incomingTraceparent
     * @return
     */
    private static <Req> Traceparent processIncomingTraceparent(Traceparent incomingTraceparent, Req request,
                                                                Getter<Req> requestHeaderGetter,
                                                                RequestTelemetry requestTelemetry) {
        Traceparent processedTraceparent = null;

        // If incoming traceparent is null create a new Traceparent
        if (incomingTraceparent == null) {

            // If BackCompat mode is enabled, read the Request-Id Header
            if (isW3CBackCompatEnabled) {
                processedTraceparent = processLegacyCorrelation(request, requestHeaderGetter, requestTelemetry);
            }

            if (processedTraceparent == null) {
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
     *
     * @param request
     * @return
     */
    private static <Req> Traceparent processLegacyCorrelation(Req request, Getter<Req> requestHeaderGetter,
                                                              RequestTelemetry requestTelemetry) {

        String requestId = requestHeaderGetter.get(request, TelemetryCorrelationUtilsCore.CORRELATION_HEADER_NAME);

        if (Strings.isNullOrEmpty(requestId)) {
            return new Traceparent();
        } else {
            String legacyOperationId = TelemetryCorrelationUtilsCore.extractRootId(requestId);
            requestTelemetry.getContext().getProperties().putIfAbsent("ai_legacyRootID", legacyOperationId);
            requestTelemetry.getContext().getOperation().setParentId(requestId);
            return new Traceparent(0, legacyOperationId, null, 0);
        }
    }

    /**
     * Helper method that extracts tracestate header from request if available and add's Azure component
     * to it. If tracestate is not available, a new tracestate with Azure component is created.
     *
     * @param request
     * @param incomingTraceparent
     * @param appId
     * @return Tracestate
     */
    @Nullable
    private static <Req> Tracestate getTracestate(Req request, Getter<Req> requestHeaderGetter,
                                                  @Nullable Traceparent incomingTraceparent, @Nullable String appId) {

        if (incomingTraceparent == null) {
            return getDefaultTracestate(appId);
        }

        String tracestateHeader = requestHeaderGetter.get(request, TRACESTATE_HEADER_NAME);
        if (Strings.isNullOrEmpty(tracestateHeader)) {
            return getDefaultTracestate(appId);
        }

        try {
            // create tracestate from incoming header
            Tracestate tracestate = Tracestate.fromString(tracestateHeader);
            if (appId != null) {
                tracestate = new Tracestate(tracestate, AZURE_TRACEPARENT_COMPONENT_INITIAL, appId);
            }
            return tracestate;
        } catch (Exception e) {
            logger.debug("Cannot parse incoming tracestate", e);
            return getDefaultTracestate(appId);
        }
    }

    private static @Nullable Tracestate getDefaultTracestate(@Nullable String appId) {
        if (appId == null) {
            return null;
        }
        try {
            return new Tracestate(null, AZURE_TRACEPARENT_COMPONENT_INITIAL, appId);
        } catch (Exception e) {
            logger.debug("cannot create default tracestate", e);
            return null;
        }
    }

    /**
     * This adds the Request-Context in response header so that the Callee can know what is the caller's AppId.
     *
     * @param response HttpResponse object
     */
    private static <Res> void addTargetAppIdInResponseHeaderViaRequestContext(Res response,
                                                                              Setter<Res> responseHeaderSetter) {
        String appId = getAppId();
        if (appId == null) {
            return;
        }
        // W3C protocol doesn't define any behavior for response headers.
        // This is purely AI concept and hence we use RequestContextHeader here.
        responseHeaderSetter.put(response, REQUEST_CONTEXT_HEADER_NAME, REQUEST_CONTEXT_HEADER_APPID_KEY + "=" + appId);
    }

    /**
     * Retrieves the appId for the current active config's instrumentation key.
     */
    public static String getAppId() {

        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey);

        // it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case, just
        // return and let the next request resolve the ikey.
        if (appId == null) {
            logger.trace("Application correlation Id could not be retrieved (task may be pending or failed)");
        }

        return appId;
    }

    /**
     * Resolves the source of a request based on request header information and the appId of the current
     * component, which is retrieved via a query to the AppInsights service.
     *
     * @param request            The servlet request.
     * @param requestTelemetry   The request telemetry in which source will be populated.
     * @param instrumentationKey The instrumentation key for the current component.
     */
    public static <Req> void resolveRequestSource(Req request, Getter<Req> requestHeaderGetter,
                                                  RequestTelemetry requestTelemetry,
                                                  @Nullable String instrumentationKey) {

        try {

            if (instrumentationKey == null || instrumentationKey.isEmpty()) {
                logger.debug("Failed to resolve correlation. InstrumentationKey is null or empty.");
                return;
            }

            String tracestate = requestHeaderGetter.get(request, TRACESTATE_HEADER_NAME);
            if (Strings.isNullOrEmpty(tracestate)) {
                if (isW3CBackCompatEnabled) {
                    String requestContext = requestHeaderGetter.get(request, REQUEST_CONTEXT_HEADER_NAME);
                    if (requestContext != null) {
                        logger.trace("Tracestate absent, In backward compatibility mode, will try to resolve " +
                                "request-context");
                        TelemetryCorrelationUtilsCore.resolveRequestSource(request, requestHeaderGetter,
                                requestTelemetry, instrumentationKey);
                        return;
                    }
                }
                logger.debug("Skip resolving request source as the following header was not found: {}",
                        TRACESTATE_HEADER_NAME);
                return;
            }

            Tracestate incomingTracestate = Tracestate.fromString(tracestate);

            String source = generateSourceTargetCorrelation(instrumentationKey,
                    incomingTracestate.get(AZURE_TRACEPARENT_COMPONENT_INITIAL));

            // Set the source of this request telemetry which would be equal to AppId of the caller if
            // it's different from current AppId or else null.
            requestTelemetry.setSource(source);

        } catch (Exception e) {
            logger.debug("Failed to resolve request source", e);
        }
    }


    /**
     * Generates the target appId to add to Outbound call
     *
     * @param requestContext
     * @return
     */
    public static String generateChildDependencyTarget(String requestContext) {
        if (requestContext == null || requestContext.isEmpty()) {
            logger.trace("generateChildDependencyTarget: won't continue as requestContext is null or empty.");
            return "";
        }

        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        if (instrumentationKey == null || instrumentationKey.isEmpty()) {
            logger.debug("Failed to generate target correlation. InstrumentationKey is null or empty.");
            return "";
        }

        // In W3C we only pass requestContext for the response. So it's expected to have only single key-value pair
        String[] keyValue = requestContext.split("=");
        assert keyValue.length == 2;

        String headerAppID = null;
        if (keyValue[0].equals(REQUEST_CONTEXT_HEADER_APPID_KEY)) {
            headerAppID = keyValue[1];
        }

        String currAppId =
                InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(TelemetryConfiguration.getActive()
                        .getInstrumentationKey());

        String target = resolve(headerAppID, currAppId);
        if (target == null) {
            logger.debug("Target value is null and hence returning empty string");
            return ""; // we want an empty string instead of null so it plays nicer with bytecode injection
        }
        return target;
    }

    /**
     * Extracts the appId/roleName out of Tracestate and compares it with the current appId. It then
     * generates the appropriate source or target.
     */
    private static String generateSourceTargetCorrelation(String instrumentationKey, String appId) {

        String myAppId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey);

        return resolve(appId, myAppId);
    }

    /**
     * Resolves appId based on appId passed in header and current appId
     *
     * @param headerAppId
     * @param currentAppId
     * @return
     */
    private static String resolve(String headerAppId, String currentAppId) {

        // it's possible the appId returned is null (e.g. async task is still pending or has failed). In this case, just
        // return and let the next request resolve the ikey.
        if (currentAppId == null) {
            logger.trace("Could not generate source/target correlation as the appId could not be resolved (e.g. " +
                    "task may be pending or failed)");
            return null;
        }

        // if the current appId and the incoming appId are the same, send null
        if (headerAppId == null || headerAppId.equals(currentAppId)) {
            return null;
        } else {
            return headerAppId;
        }
    }
}
