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
import com.microsoft.applicationinsights.agentc.internal.model.Global;
import com.microsoft.applicationinsights.agentc.internal.model.IncomingSpanImpl;
import com.microsoft.applicationinsights.agentc.internal.model.LoggerSpans;
import com.microsoft.applicationinsights.agentc.internal.model.ThreadContextImpl;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.correlation.DistributedTraceContext;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelationCore;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.MessageSupplier;
import org.glowroot.instrumentation.api.Span;
import org.glowroot.instrumentation.api.TimerName;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal.Holder;
import org.glowroot.instrumentation.engine.spi.AgentSPI;

class AgentImpl implements AgentSPI {

    private static final MessageSupplier NOP_MESSAGE_SUPPLIER = MessageSupplier.create("");

    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName, Getter<C> getter, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName,
                                      ThreadContextThreadLocal.Holder threadContextHolder, int rootNestingGroupId,
                                      int rootSuppressionKeyId) {

        TelemetryClient telemetryClient = Global.getTelemetryClient();
        if (telemetryClient == null) {
            return null;
        }
        if (transactionType.equals("ai.internal:AZUREFN")) {
            // invisible incoming spans are only used by Azure Functions
            return startAzureFnSpan(getter, carrier, threadContextHolder, rootNestingGroupId, rootSuppressionKeyId);
        }

        if (!transactionType.equals("Web") && !transactionType.equals("Background")) {
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
                TraceContextCorrelationCore.resolveCorrelationForRequest(carrier, getter, requestTelemetry, false);
        if (distributedTraceContext == null) {
            // error already logged during call to resolveCorrelationForRequest()
            return null;
        }
        TraceContextCorrelationCore.resolveRequestSource(carrier, getter, requestTelemetry, instrumentationKey);

        IncomingSpanImpl incomingSpan = new IncomingSpanImpl(transactionType, messageSupplier, threadContextHolder,
                startTimeMillis, requestTelemetry, distributedTraceContext);

        ThreadContextImpl mainThreadContext =
                new ThreadContextImpl(incomingSpan, rootNestingGroupId, rootSuppressionKeyId, false);
        threadContextHolder.set(mainThreadContext);

        return incomingSpan;
    }

    private static <C> Span startAzureFnSpan(Getter<C> getter, C carrier, Holder threadContextHolder,
                                             int rootNestingGroupId, int rootSuppressionKeyId) {

        long startTimeMillis = System.currentTimeMillis();

        RequestTelemetry requestTelemetry = new RequestTelemetry();

        DistributedTraceContext distributedTraceContext = TraceContextCorrelationCore
                .resolveCorrelationForRequest(carrier, getter, requestTelemetry, true);
        if (distributedTraceContext == null) {
            // error already logged during call to resolveCorrelationForRequest()
            return null;
        }

        IncomingSpanImpl incomingSpan = new IncomingSpanImpl(null, NOP_MESSAGE_SUPPLIER, threadContextHolder,
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
