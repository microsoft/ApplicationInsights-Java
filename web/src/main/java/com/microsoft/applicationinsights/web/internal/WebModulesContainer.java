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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Created by yonisha on 2/3/2015.
 */
public class WebModulesContainer<P, Q> {
    private List<WebTelemetryModule<P, Q>> modules = new ArrayList<>();
    private int modulesCount = 0;

    /**
     * Constructs new WebModulesContainer object from the given configuration.
     * @param configuration The configuration to take the web modules from.
     */
    public WebModulesContainer(TelemetryConfiguration configuration) {
        buildWebModules(configuration);
        this.modulesCount = modules.size();
    }

    /**
     * Invokes onBeginRequest on each of the telemetry modules
     * @param req The request to process
     * @param res The response to process
     */
    public void invokeOnBeginRequest(P req, Q res) {
        for (WebTelemetryModule<P, Q> module : modules) {
            try {
                module.onBeginRequest(req, res);
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Web module %s failed on BeginRequest with exception: %s", module.getClass().getSimpleName(), e.toString());
                InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
            }
        }
    }

    /**
     * Invokes onEndRequest on each of the telemetry modules
     * @param req The request to process
     * @param res The response to process
     */
    public void invokeOnEndRequest(P req, Q res) {
        for (WebTelemetryModule<P, Q> module : modules) {
            try {
                module.onEndRequest(req, res);
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Web module %s failed on EndRequest with exception: %s",module.getClass().getSimpleName(), e.toString());
                InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
            }
        }
    }

    /**
     * Gets the modules count
     * @return The modules count
     */
    public int getModulesCount() {
        return modulesCount;
    }

    // region Private Methods

    /**
     * Builds the web telemetry modules from the given configuration.
     * @param configuration The configuration
     */
    private void buildWebModules(TelemetryConfiguration configuration) {

        for (TelemetryModule module : configuration.getTelemetryModules()) {
            if (module instanceof WebTelemetryModule) {
                modules.add((WebTelemetryModule<P, Q>)module);
            }
        }
    }

    // endregion Private Methods
}
