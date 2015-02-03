package com.microsoft.applicationinsights.web.internal;

import javax.servlet.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.web.extensibility.WebTelemetryModule;

/**
 * Created by yonisha on 2/2/2015.
 */
public final class WebRequestTrackingFilter implements Filter {
    // region Members

    private WebModulesContainer webModulesContainer;
    private boolean isInitialized = false;

    // endregion Members

    // region Public

    /**
     * Processing the given request and response.
     * @param req The servlet request.
     * @param res The servlet response.
     * @param chain The filters chain
     * @throws IOException Exception that can be thrown from invoking the filters chain.
     * @throws ServletException Exception that can be thrown from invoking the filters chain.
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        boolean isRequestProcessedSuccessfully = true;

        if (isInitialized) {
            isRequestProcessedSuccessfully = invokeSafeOnBeginRequest(req, res);
        }

        chain.doFilter(req, res);

        if (isInitialized && isRequestProcessedSuccessfully) {
            invokeSafeOnEndRequest(req, res);
        }
    }

    /**
     * Initializes the filter from the given config.
     *
     * @param config The filter configuration.
     */
    public void init(FilterConfig config){
        TelemetryConfiguration configuration = TelemetryConfiguration.getActive();

        if (configuration == null) {
            InternalLogger.INSTANCE.log("Java SDK configuration cannot be null");

            return;
        }

        List<WebTelemetryModule> modules = (List<WebTelemetryModule>)(List<?>)configuration.getTelemetryModules();
        webModulesContainer = new WebModulesContainer(modules);

        isInitialized = true;
    }

    /**
     * Destroy the filter by releases resources.
     */
    public void destroy() {
        //add code to release any resource
    }

    // endregion Public

    // region Private

    private boolean invokeSafeOnBeginRequest(ServletRequest req, ServletResponse res) {
        boolean success = true;

        try {
            RequestTelemetryContext context = new RequestTelemetryContext(new Date().getTime());
            req.setAttribute(RequestTelemetryContext.CONTEXT_ATTR_KEY, context);

            webModulesContainer.invokeOnBeginRequest(req, res);
        } catch (Exception e) {
            InternalLogger.INSTANCE.log(
                    "Failed to invoke OnBeginRequest on telemetry modules with the following exception: %s", e.getMessage());

            success = false;
        }

        return success;
    }

    private void invokeSafeOnEndRequest(ServletRequest req, ServletResponse res) {
        try {
            webModulesContainer.invokeOnEndRequest(req, res);
        } catch (Exception e) {
            InternalLogger.INSTANCE.log(
                    "Failed to invoke OnEndRequest on telemetry modules with the following exception: %s", e.getMessage());
        }
    }

    // endregion Private
}