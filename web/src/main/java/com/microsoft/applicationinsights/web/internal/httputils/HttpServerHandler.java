package com.microsoft.applicationinsights.web.internal.httputils;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.WebModulesContainer;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.annotation.Experimental;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.util.Date;

/**
 * This Helper Handler class provides the required methods to instrument requests.
 * @param <P> The HttpRequest entity
 * @param <Q> The HttpResponse entity
 */
@Experimental
public final class HttpServerHandler<P /* >>> extends @NonNull Object */, Q> {

    /**
     * Extractor to extract data from request and response
     */
    private final HttpExtractor<P, Q> extractor;

    /**
     * Container that holds collection of
     * {@link com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule}
     */
    private final WebModulesContainer<P, Q> webModulesContainer;

    /**
     * An instance of {@link TelemetryClient} responsible to track exceptions
     */
    private final TelemetryClient telemetryClient;

    /**
     * Creates a new instance of {@link HttpServerHandler}
     *
     * @param extractor The {@code HttpExtractor} used to extract information from request and repsonse
     * @param webModulesContainer The {@code WebModulesContainer} used to hold
     *        {@link com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule}
     * @param telemetryClient The {@code TelemetryClient} used to send telemetry
     */
    public HttpServerHandler(HttpExtractor<P, Q> extractor,
        WebModulesContainer<P, Q> webModulesContainer,
        /* Nullable */ TelemetryClient telemetryClient) {
        Validate.notNull(extractor, "extractor");
        Validate.notNull(webModulesContainer, "WebModuleContainer");
        this.extractor = extractor;
        this.webModulesContainer = webModulesContainer;
        this.telemetryClient = telemetryClient;
    }

    /**
     * This method is used to instrument incoming request and initiate correlation with help of
     * {@link com.microsoft.applicationinsights.web.extensibility.modules.WebRequestTrackingTelemetryModule#onBeginRequest(HttpServletRequest, HttpServletResponse)}
     * @param request incoming Request
     * @param response Response object
     * @return {@link RequestTelemetryContext} that contains correlation information and metadata about request
     * @throws MalformedURLException
     */
    public RequestTelemetryContext handleStart(P request, Q response) throws MalformedURLException {
        RequestTelemetryContext context = new RequestTelemetryContext(new Date().getTime(),null);
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
        ThreadContext.setRequestTelemetryContext(context);
        String method = extractor.getMethod(request);
        String userAgent = extractor.getUserAgent(request);
        String uriWithoutSessionId = extractor.getUri(request);
        String scheme = extractor.getScheme(request);
        String host = extractor.getHost(request);
        String query = extractor.getQuery(request);
        if (!CommonUtils.isNullOrEmpty(query)) {
            requestTelemetry.setUrl(scheme + "://" + host + uriWithoutSessionId + "?" + query);
        } else {
            requestTelemetry.setUrl(scheme + "://" + host + uriWithoutSessionId);
        }
        requestTelemetry.setHttpMethod(method);
        requestTelemetry.setName(method + " " + uriWithoutSessionId);
        requestTelemetry.getContext().getUser().setUserAgent(userAgent);
        requestTelemetry.setTimestamp(new Date(context.getRequestStartTimeTicks()));
        webModulesContainer.invokeOnBeginRequest(request, response);
        return context;
    }

    /**
     * This method is used to indicate request end instrumentation, complete correlation and record timing, response
     * @param request HttpRequest object
     * @param response HttpResponse object
     */
    public void handleEnd(P request, Q response) {
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
        long endTime = new Date().getTime();
        requestTelemetry.setDuration(new Duration(endTime - context.getRequestStartTimeTicks()));
        int resultCode = extractor.getStatusCode(response);
        requestTelemetry.setSuccess(resultCode < 400);
        requestTelemetry.setResponseCode(Integer.toString(resultCode));
        webModulesContainer.invokeOnEndRequest(request, response);
    }

    /**
     * This method is used to capture runtime exceptions while processing request
     * @param e Exception occurred
     */
    public void handleException(Exception e) {
        try {
            InternalLogger.INSTANCE.trace("Unhandled exception while processing request: %s",
                ExceptionUtils.getStackTrace(e));
            if (telemetryClient != null) {
                telemetryClient.trackException(e);
            }
        } catch (Exception ex) {
            // swallow AI Exception
        }
    }
}
