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

import javax.annotation.Nullable;

public interface AgentBridge<T> {

    boolean isAgentRunning();

    AgentBinding bindToThread(T requestTelemetryContext, @Nullable ServletRequestInfo servletRequestInfo);

    class ServletRequestInfo {

        private final String method;
        private final String contextPath;
        private final String servletPath;
        private final @Nullable String pathInfo;
        private final String uri;

        public ServletRequestInfo(String method, String contextPath, String servletPath, @Nullable String pathInfo, String uri) {
            this.method = method;
            this.contextPath = contextPath;
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
            this.uri = uri;
        }

        public String getMethod() {
            return method;
        }

        public String getContextPath() {
            return contextPath;
        }

        public String getServletPath() {
            return servletPath;
        }

        @Nullable
        public String getPathInfo() {
            return pathInfo;
        }

        public String getUri() {
            return uri;
        }
    }
}
