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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.agent.AbstractSdkBridge;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtils;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelation;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Traceparent;

class SdkBridgeImpl extends AbstractSdkBridge<RequestTelemetryContext> {

    SdkBridgeImpl(TelemetryClient client) {
        super(client);
    }

    @Override
    public void bindRequestTelemetryContext(RequestTelemetryContext requestTelemetryContext) {
        ThreadContext.setRequestTelemetryContext(requestTelemetryContext);
    }

    @Override
    public void unbindRequestTelemetryContext() {
        ThreadContext.remove();
    }

    @Override
    public void setOperationName(RequestTelemetryContext requestTelemetryContext, String operationName) {
        RequestTelemetry requestTelemetry = requestTelemetryContext.getHttpRequestTelemetry();
        if (requestTelemetry.isAllowAgentToOverrideName()) {
            requestTelemetry.setName(operationName);
        }
    }

    @Override
    public String generateChildDependencyTarget(String requestContext, boolean w3c) {
        if (w3c) {
            return TraceContextCorrelation.generateChildDependencyTarget(requestContext);
        } else {
            return TelemetryCorrelationUtils.generateChildDependencyTarget(requestContext);
        }
    }

    @Override
    public <C> String propagate(Setter<C> setter, C carrier, boolean w3c, boolean w3cBackCompat) {
        if (w3c) {
            Traceparent traceparent = TraceContextCorrelation.generateChildDependencyTraceparentObj();
            if (traceparent == null) {
                // this means an error occurred (and was logged) in above method, so just return a valid outgoingSpanId
                return new Traceparent().getSpanId();
            }
            String tracestate = TraceContextCorrelation.retriveTracestate();
            setter.put(carrier, "traceparent", traceparent.toString());
            if (w3cBackCompat) {
                setter.put(carrier, "Request-Id", "|" + traceparent.getTraceId() + "." + traceparent.getSpanId() + ".");
                setter.put(carrier, "Request-Context", TelemetryCorrelationUtils.retrieveApplicationCorrelationId());
            }
            if (tracestate != null) {
                setter.put(carrier, "tracestate", tracestate);
            }
            return traceparent.getSpanId();
        } else {
            String outgoingSpanId = TelemetryCorrelationUtils.generateChildDependencyId();
            String correlationContext = TelemetryCorrelationUtils.retrieveCorrelationContext();
            String appCorrelationId = TelemetryCorrelationUtils.retrieveApplicationCorrelationId();
            setter.put(carrier, "Request-Id", outgoingSpanId);
            setter.put(carrier, "Correlation-Context", correlationContext);
            setter.put(carrier, "Request-Context", appCorrelationId);
            return outgoingSpanId;
        }
    }
}
