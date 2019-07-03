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

package com.microsoft.applicationinsights.agentc.internal.model;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.CorrelationContext;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Traceparent;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DistributedTraceContext {

    private final RequestTelemetry requestTelemetry;

    // w3c format
    private final @Nullable Tracestate tracestate;
    private final int traceflag;

    // ApplicationInsights format
    private final @Nullable CorrelationContext correlationContext;
    private final @Nullable AtomicInteger currentChildId;

    // w3c format
    public DistributedTraceContext(RequestTelemetry requestTelemetry, Tracestate tracestate, int traceflag) {
        this.requestTelemetry = requestTelemetry;
        this.tracestate = tracestate;
        this.traceflag = traceflag;
        correlationContext = null;
        currentChildId = null;
    }

    // ApplicationInsights format
    public DistributedTraceContext(RequestTelemetry requestTelemetry, CorrelationContext correlationContext) {
        this.requestTelemetry = requestTelemetry;
        this.correlationContext = correlationContext;
        currentChildId = new AtomicInteger();
        tracestate = null;
        traceflag = 0;
    }

    // w3c format
    String generateChildDependencyTraceparent() {

        String traceId = requestTelemetry.getContext().getOperation().getId();
        Traceparent tp = new Traceparent(0, traceId, null, traceflag);

        return tp.toString();
    }

    // w3c format
    String retrieveTracestate() {
        Preconditions.checkNotNull(tracestate);
        return tracestate.toString();
    }

    // ApplicationInsights format
    String generateChildDependencyId() {

        String parentId = requestTelemetry.getContext().getOperation().getParentId();

        if (parentId != null && parentId.length() > 0 && parentId.charAt(0) != '|') {
            // incoming requestId does not follow hierarchical convention, so do not modify the child IDs
            return requestTelemetry.getContext().getOperation().getParentId();
        } else {
            return requestTelemetry.getId() + currentChildId.incrementAndGet() + ".";
        }
    }

    // ApplicationInsights format
    String retrieveCorrelationContext() {
        return correlationContext.toString();
    }
}
