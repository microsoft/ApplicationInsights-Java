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

package com.microsoft.applicationinsights.web.extensibility.modules;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtils;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by yonisha on 2/2/2015.
 */
public class WebRequestTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule {

    // region Members

    private TelemetryClient telemetryClient;
    private boolean isInitialized = false;

    public boolean isW3CEnabled = false;

    /**
     * Tag to indicate if W3C tracing protocol is enabled.
     */
    private final String W3C_CONFIGURATION_PARAMETER = "W3CEnabled";

    /**
     * Tag to indicate if backward compatibility is turned ON/OFF for W3C.
     * By default backward compatibility mode is turned ON.
     */
    private final String W3C_BACKCOMPAT_PARAMETER = "enableW3CBackCompat";

    // endregion Members

    // region Public

    public WebRequestTrackingTelemetryModule() {}

    /**
     * Ctor that parses incoming configuration.
     * @param configurationData SDK config Object
     */
    public WebRequestTrackingTelemetryModule(Map<String, String> configurationData) {
        if (configurationData == null) {
            throw new NullPointerException("configurationData should not be null");
        }

        if (configurationData.containsKey(W3C_CONFIGURATION_PARAMETER)) {
            isW3CEnabled = Boolean.valueOf(configurationData.get(W3C_CONFIGURATION_PARAMETER));
            InternalLogger.INSTANCE.trace(String.format("Inbound W3C tracing mode is enabled %s", isW3CEnabled));
        }

        if (configurationData.containsKey(W3C_BACKCOMPAT_PARAMETER)) {
            boolean enableBackCompatibilityForW3C = Boolean.valueOf(configurationData.get(
                W3C_BACKCOMPAT_PARAMETER
            ));
            TraceContextCorrelation.setIsW3CBackCompatEnabled(enableBackCompatibilityForW3C);
        }


    }

    /**
     * Used for SpringBoot setttings to propogate the switch for W3C to TracecontextCorrelation class
     * @param enableBackCompatibilityForW3C
     */
    public void setEnableBackCompatibilityForW3C(boolean enableBackCompatibilityForW3C) {
        TraceContextCorrelation.setIsW3CBackCompatEnabled(enableBackCompatibilityForW3C);
    }

    /**
     * Begin request processing.
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onBeginRequest(ServletRequest req, ServletResponse res) {
        if (!isInitialized) {
            // Avoid logging to not spam the log. It is sufficient that the module initialization failure
            // has been logged.
            return;
        }

        try {
            resolveCorrelation(req, res);

        } catch (Exception e) {
            String moduleClassName = this.getClass().getSimpleName();
            InternalLogger.INSTANCE.error("Telemetry module %s onBeginRequest failed with exception: %s", moduleClassName, e.toString());
        }
    }

    private void resolveCorrelation(ServletRequest req, ServletResponse res) {
        if (!(req instanceof HttpServletRequest && res instanceof HttpServletResponse)) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();
        RequestTelemetry telemetry = context.getHttpRequestTelemetry();

        // Look for cross-component correlation headers and resolve correlation ID's
        if (isW3CEnabled) {
            TraceContextCorrelation.resolveCorrelation(request, response, telemetry);
        } else {
            // Default correlation experience
            TelemetryCorrelationUtils.resolveCorrelation(request, response, telemetry);
        }
    }

    /**
     * End request processing.
     * @param req The request to process
     * @param res The response to modify
     */
    @Override
    public void onEndRequest(ServletRequest req, ServletResponse res) {
        if (!isInitialized) {
            // Avoid logging to not spam the log. It is sufficient that the module initialization failure
            // has been logged.
            return;
        }

        try {
            RequestTelemetry telemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();

            resolveRequestSource(req, telemetry);

            telemetryClient.track(telemetry);
        } catch (Exception e) {
            String moduleClassName = this.getClass().getSimpleName();
            InternalLogger.INSTANCE.error("Telemetry module %s onEndRequest failed with exception: %s", moduleClassName, e.toString());
        }
    }

    private void resolveRequestSource(ServletRequest req, RequestTelemetry telemetry) {
        HttpServletRequest request = (HttpServletRequest) req;
        String instrumentationKey = this.telemetryClient.getContext().getInstrumentationKey();
        if (isW3CEnabled) {
            TraceContextCorrelation.resolveRequestSource(request, telemetry, instrumentationKey);
        } else {
            TelemetryCorrelationUtils.resolveRequestSource(request, telemetry, instrumentationKey);
        }
    }

    /**
     * Initializes the telemetry module with the given telemetry configuration.
     * @param configuration The telemetry configuration.
     */
    @Override
    public void initialize(TelemetryConfiguration configuration) {
        try {
            telemetryClient = new TelemetryClient(configuration);
            isInitialized = true;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to initialize telemetry module %s. Exception: %s.", this.getClass().getSimpleName(), e.toString());
        }
    }

    // endregion Public
}
