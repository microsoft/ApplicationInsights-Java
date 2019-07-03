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

import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.web.internal.correlation.InstrumentationKeyResolver;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.AsyncQuerySpan;
import org.glowroot.instrumentation.api.AsyncSpan;
import org.glowroot.instrumentation.api.AuxThreadContext;
import org.glowroot.instrumentation.api.Getter;
import org.glowroot.instrumentation.api.MessageSupplier;
import org.glowroot.instrumentation.api.QueryMessageSupplier;
import org.glowroot.instrumentation.api.QuerySpan;
import org.glowroot.instrumentation.api.Setter;
import org.glowroot.instrumentation.api.Span;
import org.glowroot.instrumentation.api.Timer;
import org.glowroot.instrumentation.api.TimerName;
import org.glowroot.instrumentation.api.internal.ReadableMessage;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.instrumentation.engine.impl.NopTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThreadContextImpl implements ThreadContextPlus {

    private static final Logger logger = LoggerFactory.getLogger(ThreadContextImpl.class);

    public static final String REQUEST_CONTEXT_HEADER_APPID_KEY = "appId";

    private final IncomingSpanImpl incomingSpan;

    private int currentNestingGroupId;
    private int currentSuppressionKeyId;

    private final @Nullable TwoPartCompletion auxThreadAsyncCompletion;

    public ThreadContextImpl(IncomingSpanImpl incomingSpan, int rootNestingGroupId, int rootSuppressionKeyId,
                             boolean auxThread) {
        this.incomingSpan = incomingSpan;
        currentNestingGroupId = rootNestingGroupId;
        currentSuppressionKeyId = rootSuppressionKeyId;
        auxThreadAsyncCompletion = auxThread ? new TwoPartCompletion() : null;
    }

    IncomingSpanImpl getIncomingSpan() {
        return incomingSpan;
    }

    @Override
    public boolean isInTransaction() {
        return true;
    }

    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName, Getter<C> getter, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName,
                                      AlreadyInTransactionBehavior alreadyInTransactionBehavior) {
        // ApplicationInsights doesn't currently support local spans
        return NopTransactionService.LOCAL_SPAN;
    }

    @Override
    public Span startLocalSpan(MessageSupplier messageSupplier, TimerName timerName) {
        String text = ((ReadableMessage) messageSupplier.get()).getText();
        if (text.startsWith(LocalSpanImpl.PREFIX)) {
            return new LocalSpanImpl(incomingSpan.getOperationId(), incomingSpan.getOperationParentId(), text,
                    System.currentTimeMillis(), messageSupplier);
        } else {
            return NopTransactionService.LOCAL_SPAN;
        }
    }

    @Override
    public QuerySpan startQuerySpan(String type, String dest, String text, QueryMessageSupplier queryMessageSupplier,
                                    TimerName timerName) {
        return new QuerySpanImpl(incomingSpan.getOperationId(), incomingSpan.getOperationParentId(), type, dest, text,
                queryMessageSupplier, System.currentTimeMillis());
    }

    @Override
    public QuerySpan startQuerySpan(String type, String dest, String text, long queryExecutionCount,
                                    QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return new QuerySpanImpl(incomingSpan.getOperationId(), incomingSpan.getOperationParentId(), type, dest, text,
                queryMessageSupplier, System.currentTimeMillis());
    }

    @Override
    public AsyncQuerySpan startAsyncQuerySpan(String type, String dest, String text,
                                              QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return new AsyncQuerySpanImpl(incomingSpan.getOperationParentId(), incomingSpan.getOperationId(), type, dest,
                text, queryMessageSupplier, System.currentTimeMillis());
    }

    @Override
    public <C> Span startOutgoingSpan(String type, String text, Setter<C> setter, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName) {
        // TODO revisit the point of text
        String outgoingSpanId = propagate(setter, carrier);
        return new OutgoingSpanImpl(incomingSpan.getOperationId(), incomingSpan.getOperationParentId(), outgoingSpanId,
                type, text, System.currentTimeMillis(), messageSupplier);
    }

    @Override
    public <C> AsyncSpan startAsyncOutgoingSpan(String type, String text, Setter<C> setter, C carrier,
                                                MessageSupplier messageSupplier, TimerName timerName) {
        // TODO revisit the point of text
        String outgoingSpanId = propagate(setter, carrier);
        return new AsyncOutgoingSpanImpl(incomingSpan.getOperationParentId(), incomingSpan.getOperationId(),
                outgoingSpanId, type, text, System.currentTimeMillis(), messageSupplier);
    }

    @Override
    public void captureLoggerSpan(MessageSupplier messageSupplier, @Nullable Throwable throwable) {
        LoggerSpans
                .track(incomingSpan.getOperationId(), incomingSpan.getOperationParentId(), messageSupplier, throwable,
                        System.currentTimeMillis());
    }

    @Override
    public Timer startTimer(TimerName timerName) {
        // timers are not used by ApplicationInsights
        return NopTransactionService.TIMER;
    }

    @Override
    public AuxThreadContext createAuxThreadContext() {
        return new AuxThreadContextImpl(incomingSpan);
    }

    @Override
    public void setTransactionAsync() {
        incomingSpan.setAsync();
    }

    @Override
    public void setTransactionAsyncComplete() {
        if (auxThreadAsyncCompletion == null || auxThreadAsyncCompletion.setPart1()) {
            incomingSpan.setAsyncComplete();
        }
    }

    @Override
    public void setTransactionType(@Nullable String transactionType, int priority) {
        // the core instrumentation only use this method to set transaction type to "Synthetic"
        // (when the "X-Glowroot-Transaction-Type" header is set to "Synthetic")
    }

    @Override
    public void setTransactionName(@Nullable String transactionName, int priority) {
        // currently ignoring priority, which is ok since just using core instrumentation
        if (transactionName != null) {
            incomingSpan.setTransactionName(transactionName);
        }
    }

    @Override
    public void setTransactionUser(@Nullable String user, int priority) {
    }

    @Override
    public void addTransactionAttribute(String name, @Nullable String value) {
        // the core instrumentation doesn't call this
    }

    @Override
    public void setTransactionSlowThreshold(long threshold, TimeUnit unit, int priority) {
        // the core instrumentation only calls this to set slow threshold to zero for Startup spans
    }

    @Override
    public void setTransactionError(Throwable t) {
        incomingSpan.setException(t);
    }

    @Override
    public void setTransactionError(@Nullable String message) {
        // TODO revisit
    }

    @Override
    public void setTransactionError(@Nullable String message, @Nullable Throwable t) {
        incomingSpan.setException(t);
    }

    @Override
    public void trackResourceAcquired(Object resource, boolean withLocationStackTrace) {
        // not supported for now
    }

    @Override
    public void trackResourceReleased(Object resource) {
        // not supported for now
    }

    @Override
    public @Nullable ServletRequestInfo getServletRequestInfo() {
        return incomingSpan.getServletRequestInfo();
    }

    @Override
    public void setServletRequestInfo(ServletRequestInfo servletRequestInfo) {
        incomingSpan.setServletRequestInfo(servletRequestInfo);
    }

    @Override
    public int getCurrentNestingGroupId() {
        return currentNestingGroupId;
    }

    @Override
    public void setCurrentNestingGroupId(int nestingGroupId) {
        this.currentNestingGroupId = nestingGroupId;
    }

    @Override
    public int getCurrentSuppressionKeyId() {
        return currentSuppressionKeyId;
    }

    @Override
    public void setCurrentSuppressionKeyId(int suppressionKeyId) {
        this.currentSuppressionKeyId = suppressionKeyId;
    }

    public void endAuxThreadContext() {
        checkNotNull(auxThreadAsyncCompletion);
        if (auxThreadAsyncCompletion.setPart2()) {
            incomingSpan.setAsyncComplete();
        }
    }

    private <C> String propagate(Setter<C> setter, C carrier) {
        DistributedTraceContext distributedTraceContext = incomingSpan.getDistributedTraceContext();
        if (Global.isOutboundW3CEnabled()) {
            String traceparent = distributedTraceContext.generateChildDependencyTraceparent();
            String tracestate = distributedTraceContext.retrieveTracestate();
            String outgoingSpanId = createChildIdFromTraceparentString(traceparent);
            setter.put(carrier, "traceparent", traceparent);
            if (Global.isOutboundW3CBackCompatEnabled()) {
                setter.put(carrier, "Request-Id", outgoingSpanId);
            }
            if (tracestate != null) {
                setter.put(carrier, "tracestate", tracestate);
            }
            return outgoingSpanId;
        } else {
            String outgoingSpanId = distributedTraceContext.generateChildDependencyId();
            String correlationContext = distributedTraceContext.retrieveCorrelationContext();
            String appCorrelationId = retrieveApplicationCorrelationId();
            setter.put(carrier, "Request-Id", outgoingSpanId);
            setter.put(carrier, "Correlation-Context", correlationContext);
            setter.put(carrier, "Request-Context", appCorrelationId);
            return outgoingSpanId;
        }
    }

    // see TraceContextCorrelation.createChildIdFromTraceparentString()
    private static String createChildIdFromTraceparentString(String traceparent) {
        String[] traceparentArr = traceparent.split("-");
        return "|" + traceparentArr[1] + "." + traceparentArr[2] + ".";
    }

    // see TelemetryCorrelationUtils.retrieveApplicationCorrelationId()
    private static String retrieveApplicationCorrelationId() {

        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String appId = InstrumentationKeyResolver.INSTANCE.resolveInstrumentationKey(instrumentationKey);

        if (appId == null) {
            // async task is still pending or has failed, return and let the next request resolve the ikey
            logger.trace("Application correlation Id could not be retrieved (e.g. task may be pending or failed)");
            return "";
        }

        return REQUEST_CONTEXT_HEADER_APPID_KEY + "=" + appId;
    }
}
