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

package com.microsoft.applicationinsights.internal.agent;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.microsoft.applicationinsights.agent.internal.sdk.AgentBridgeInternal;
import com.microsoft.applicationinsights.agent.internal.sdk.BindingResult;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge;
import org.glowroot.instrumentation.api.ThreadContext;

class AgentBridgeImpl<T> implements AgentBridge<T> {

    private final SdkBridge<T> sdkBridge;

    AgentBridgeImpl(SdkBridge<T> sdkBridge) {
        this.sdkBridge = sdkBridge;
    }

    @Override
    public boolean isAgentRunning() {
        return true;
    }

    @Override
    public AgentBinding bindToThread(T requestTelemetryContext, @Nullable ServletRequestInfo servletRequestInfo) {
        return new AgentBindingImpl(AgentBridgeInternal.bindToThread(sdkBridge, requestTelemetryContext,
                new ServletRequestInfoImpl(servletRequestInfo)));
    }

    private static class AgentBindingImpl implements AgentBinding {

        private final BindingResult bindingResult;

        AgentBindingImpl(BindingResult bindingResult) {
            this.bindingResult = bindingResult;
        }

        public void unbindFromMainThread() {
            bindingResult.unbindFromMainThread();
        }

        public void unbindFromRunawayChildThreads() {
            bindingResult.unbindFromRunawayChildThreads();
        }
    }

    private static class ServletRequestInfoImpl implements ThreadContext.ServletRequestInfo {

        private final ServletRequestInfo servletRequestInfo;
        private final List<String> jaxRsParts = new ArrayList<>();

        private ServletRequestInfoImpl(ServletRequestInfo servletRequestInfo) {
            this.servletRequestInfo = servletRequestInfo;
        }

        @Override
        public String getMethod() {
            return servletRequestInfo.getMethod();
        }

        @Override
        public String getContextPath() {
            return servletRequestInfo.getContextPath();
        }

        @Override
        public String getServletPath() {
            return servletRequestInfo.getServletPath();
        }

        @Override
        public @Nullable String getPathInfo() {
            return servletRequestInfo.getPathInfo();
        }

        @Override
        public String getUri() {
            return servletRequestInfo.getUri();
        }

        @Override
        public void addJaxRsPart(String part) {
            jaxRsParts.add(part);
        }

        @Override
        public List<String> getJaxRsParts() {
            return jaxRsParts;
        }
    }
}
