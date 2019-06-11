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

package com.microsoft.applicationinsights.internal.config;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

public class AgentXmlElement {

    @XStreamImplicit
    @XStreamAlias("Instrumentation")
    private List<InstrumentationXmlElement> instrumentation = new ArrayList<>();

    @XStreamAlias("DistributedTracing")
    private DistributedTracingXmlElement distributedTracing = new DistributedTracingXmlElement();

    @XStreamAlias("UserTracking")
    private UserTrackingXmlElement userTracking = new UserTrackingXmlElement();

    @XStreamAlias("SessionTracking")
    private SessionTrackingXmlElement sessionTracking = new SessionTrackingXmlElement();

    public List<InstrumentationXmlElement> getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(List<InstrumentationXmlElement> instrumentation) {
        this.instrumentation = instrumentation;
    }

    public DistributedTracingXmlElement getDistributedTracing() {
        return distributedTracing;
    }

    public void setDistributedTracing(DistributedTracingXmlElement distributedTracing) {
        this.distributedTracing = distributedTracing;
    }

    public UserTrackingXmlElement getUserTracking() {
        return userTracking;
    }

    public void setUserTracking(UserTrackingXmlElement userTracking) {
        this.userTracking = userTracking;
    }

    public SessionTrackingXmlElement getSessionTracking() {
        return sessionTracking;
    }

    public void setSessionTracking(SessionTrackingXmlElement sessionTracking) {
        this.sessionTracking = sessionTracking;
    }

    public static class InstrumentationXmlElement {

        @XStreamAsAttribute
        private String name;

        @XStreamAsAttribute
        private boolean enabled = true;

        @XStreamImplicit
        @XStreamAlias("Param")
        private List<ParamXmlElement> params = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<ParamXmlElement> getParams() {
            return params;
        }

        public void setParams(List<ParamXmlElement> params) {
            this.params = params;
        }
    }

    public static class DistributedTracingXmlElement {

        @XStreamAlias("W3C")
        private W3CXmlElement w3c = new W3CXmlElement();

        public W3CXmlElement getW3c() {
            return w3c;
        }

        public void setW3c(W3CXmlElement w3c) {
            this.w3c = w3c;
        }
    }

    public static class W3CXmlElement {

        @XStreamAsAttribute
        private boolean enabled;

        @XStreamAsAttribute
        private boolean backCompatEnabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBackCompatEnabled() {
            return backCompatEnabled;
        }

        public void setBackCompatEnabled(boolean backCompatEnabled) {
            this.backCompatEnabled = backCompatEnabled;
        }
    }

    public static class UserTrackingXmlElement {

        @XStreamAsAttribute
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class SessionTrackingXmlElement {

        @XStreamAsAttribute
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
