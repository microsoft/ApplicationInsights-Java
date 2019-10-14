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

package com.microsoft.applicationinsights.agentc.internal;

import java.util.Date;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agentc.internal.model.DistributedTraceContext;
import com.microsoft.applicationinsights.agentc.internal.model.Global;
import com.microsoft.applicationinsights.agentc.internal.model.IncomingSpanImpl;
import com.microsoft.applicationinsights.agentc.internal.model.LoggerSpans;
import com.microsoft.applicationinsights.agentc.internal.model.ThreadContextImpl;
import com.microsoft.applicationinsights.agentc.internal.model.TraceContextCorrelationCore;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.MessageSupplier;
import org.glowroot.instrumentation.api.Span;
import org.glowroot.instrumentation.api.TimerName;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.instrumentation.engine.spi.AgentSPI;

class AgentImpl implements AgentSPI {

    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName, Getter<C> getter, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName,
                                      ThreadContextThreadLocal.Holder threadContextHolder, int rootNestingGroupId,
                                      int rootSuppressionKeyId) {

        TelemetryClient telemetryClient = Global.getTelemetryClient();
        if (telemetryClient == null
                || !transactionType.equals("Web") && !transactionType.equals("Background")) {
            return null;
        }

        long startTimeMillis = System.currentTimeMillis();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        requestTelemetry.setName(transactionName);
        requestTelemetry.getContext().getOperation().setName(transactionName);
        requestTelemetry.setTimestamp(new Date(startTimeMillis));

        String userAgent = getter.get(carrier, "User-Agent");
        requestTelemetry.getContext().getUser().setUserAgent(userAgent);

        String instrumentationKey = telemetryClient.getContext().getInstrumentationKey();
        DistributedTraceContext distributedTraceContext =
                TraceContextCorrelationCore.resolveCorrelationForRequest(carrier, getter, requestTelemetry);
        TraceContextCorrelationCore.resolveRequestSource(carrier, getter, requestTelemetry, instrumentationKey);
        if (requestTelemetry.getContext().getOperation().getParentId() == null) {
            requestTelemetry.getContext().getOperation().setParentId(requestTelemetry.getId());
        }

        IncomingSpanImpl incomingSpan = new IncomingSpanImpl(transactionType, messageSupplier, threadContextHolder,
                startTimeMillis, requestTelemetry, distributedTraceContext);

        ThreadContextImpl mainThreadContext =
                new ThreadContextImpl(incomingSpan, rootNestingGroupId, rootSuppressionKeyId, false);
        threadContextHolder.set(mainThreadContext);

        return incomingSpan;
    }

    @Override
    public void captureLoggerSpan(MessageSupplier messageSupplier, @Nullable Throwable throwable) {
        LoggerSpans.track("", "", messageSupplier, throwable, System.currentTimeMillis());
    }
}
