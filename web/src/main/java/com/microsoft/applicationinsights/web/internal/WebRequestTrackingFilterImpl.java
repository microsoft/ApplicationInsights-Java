/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.web.internal;

import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.AgentTLS;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;

/**
 * Created by yonisha on 2/2/2015.
 */
public class WebRequestTrackingFilterImpl implements Filter {
    // region Members

    private WebModulesContainer webModulesContainer;
    private boolean isInitialized = false;
    private TelemetryClient telemetryClient;
    private String key;
    private boolean agentIsUp = false;

    // endregion Members

    // region Public


    public void setKey(String key) {
        try {
            AgentTLS.getTLSKey();
            agentIsUp = true;
        } catch (Throwable throwable) {
            agentIsUp = false;
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "setKey: Failed to find AgentTLS");
        }
        this.key = key;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Processing the given request and response.
     * @param req The servlet request.
     * @param res The servlet response.
     * @param chain The filters chain
     * @throws java.io.IOException Exception that can be thrown from invoking the filters chain.
     * @throws javax.servlet.ServletException Exception that can be thrown from invoking the filters chain.
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        boolean isRequestProcessedSuccessfully = true;

        setKeyOnTLS();

        if (isInitialized) {
            isRequestProcessedSuccessfully = invokeSafeOnBeginRequest(req, res);
        }

        try {
            chain.doFilter(req, res);
        } catch (ServletException se) {
            onException(se);
            throw se;
        } catch (IOException ioe) {
            onException(ioe);
            throw ioe;
        } catch (RuntimeException re) {
            onException(re);
            throw re;
        }

        if (isInitialized && isRequestProcessedSuccessfully) {
            invokeSafeOnEndRequest(req, res);
        }
    }

    private void onException(Exception e) {
        try {
            InternalLogger.INSTANCE.trace("Unhandled application exception: %s", e.getMessage());
            if (telemetryClient != null) {
                telemetryClient.trackException(e);
            }
        } catch (Throwable t) {
        }
    }

    /**
     * Initializes the filter from the given config.
     * @param config The filter configuration.
     */
    public void init(FilterConfig config){
        try {
            if (!ClassDataUtils.INSTANCE.verifyMethodExists(javax.servlet.http.HttpServletResponse.class, "getStatus")) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR,
                        "Unsupported servlet version. The Application Insights web request tracking filter requires the provided javax.servlet library to implement servlet spec 3.0 or above. The web request tracking filter will be disabled.");

                return;
            }

            TelemetryConfiguration configuration = TelemetryConfiguration.getActive();

            if (configuration == null) {
                InternalLogger.INSTANCE.error(
                        "Java SDK configuration cannot be null. Web request tracking filter will be disabled.");

                return;
            }

            telemetryClient = new TelemetryClient(configuration);
            webModulesContainer = new WebModulesContainer(configuration);
            isInitialized = true;
        } catch (Throwable t) {
            String filterName = this.getClass().getSimpleName();
            InternalLogger.INSTANCE.error(
                    "Application Insights filter %s has been failed to initialized.\n" +
                            "Web request tracking filter will be disabled. Exception: %s", filterName, t.getMessage());
        }
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
            ThreadContext.setRequestTelemetryContext(context);

            webModulesContainer.invokeOnBeginRequest(req, res);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to invoke OnBeginRequest on telemetry modules with the following exception: %s", e.getMessage());

            success = false;
        }

        return success;
    }

    private void invokeSafeOnEndRequest(ServletRequest req, ServletResponse res) {
        try {
            webModulesContainer.invokeOnEndRequest(req, res);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to invoke OnEndRequest on telemetry modules with the following exception: %s", e.getMessage());
        }
    }

    private void setKeyOnTLS() {
        if (agentIsUp) {
            try {
                AgentTLS.setTLSKey(key);
            } catch (Throwable e) {
                if (e instanceof ClassNotFoundException ||
                        e instanceof NoClassDefFoundError) {

                    // This means that the Agent is not present and therefore we will stop trying
                    agentIsUp = false;
                    InternalLogger.INSTANCE.error("setKeyOnTLS: Failed to find AgentTLS: '%s'", e.getMessage());
                }
            }
        }
    }
    // endregion Private
}
