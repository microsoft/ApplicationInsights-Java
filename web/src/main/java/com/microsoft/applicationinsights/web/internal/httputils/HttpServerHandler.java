package com.microsoft.applicationinsights.web.internal.httputils;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.WebModulesContainer;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.MalformedURLException;
import java.util.Date;

public final class HttpServerHandler<P /* >>> extends @NonNull Object */, Q> {

    private final HttpExtractor<P, Q> extractor;

    private final WebModulesContainer<P, Q> webModulesContainer;

    private final TelemetryClient telemetryClient;

    public HttpServerHandler(HttpExtractor<P, Q> extractor,
                             /* Nullable */  WebModulesContainer<P, Q> webModulesContainer,
                             TelemetryClient telemetryClient) {
        Validate.notNull(extractor, "extractor");
        this.extractor = extractor;
        this.webModulesContainer = webModulesContainer;
        this.telemetryClient = telemetryClient;
    }

    public RequestTelemetryContext handleStart(P request, Q response) throws MalformedURLException {
        RequestTelemetryContext context = new RequestTelemetryContext(new Date().getTime(),null);
        RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
        ThreadContext.setRequestTelemetryContext(context);

        // String rURI = request.getRequestURI();
        String method = extractor.getMethod(request);
        String userAgent = extractor.getUserAgent(request);
        String url = extractor.getUrl(request);

        requestTelemetry.setHttpMethod(method);
        requestTelemetry.setUrl(extractor.getUrl(request));


        //String rUriWithoutSessionId = removeSessionIdFromUri(rURI);
        requestTelemetry.setName(String.format("%s %s", method, url));
        requestTelemetry.getContext().getUser().setUserAgent(userAgent);
        requestTelemetry.setTimestamp(new Date(context.getRequestStartTimeTicks()));

        webModulesContainer.invokeOnBeginRequest(request, response);

        return context;
    }

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

    public void handleException(Exception e) {
        try {
            InternalLogger.INSTANCE.trace("Unhandled application exception: %s", ExceptionUtils.getStackTrace(e));
            if (telemetryClient != null) {
                telemetryClient.trackException(e);
            }

        } catch (Exception ex) {

        }

    }
}
