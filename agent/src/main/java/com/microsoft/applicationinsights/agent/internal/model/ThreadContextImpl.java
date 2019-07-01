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

package com.microsoft.applicationinsights.agent.internal.model;

import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.agent.internal.sdk.SdkBinding;
import com.microsoft.applicationinsights.agent.internal.sdk.SdkBridge;
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
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.instrumentation.engine.impl.NopTransactionService;

public class ThreadContextImpl<T> implements ThreadContextPlus {

    private final SdkBinding<T> sdkBinding;
    private final @Nullable ServletRequestInfo servletRequestInfo;

    private int currentNestingGroupId;
    private int currentSuppressionKeyId;

    public ThreadContextImpl(SdkBinding<T> sdkBinding, @Nullable ServletRequestInfo servletRequestInfo,
                             int rootNestingGroupId, int rootSuppressionKeyId) {
        this.sdkBinding = sdkBinding;
        this.servletRequestInfo = servletRequestInfo;
        currentNestingGroupId = rootNestingGroupId;
        currentSuppressionKeyId = rootSuppressionKeyId;
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
        return NopTransactionService.LOCAL_SPAN;
    }

    @Override
    public QuerySpan startQuerySpan(String type, String dest, String text, QueryMessageSupplier queryMessageSupplier,
                                    TimerName timerName) {
        return new QuerySpanImpl(sdkBinding.getSdkBridge(), type, dest, text, queryMessageSupplier,
                System.currentTimeMillis());
    }

    @Override
    public QuerySpan startQuerySpan(String type, String dest, String text, long queryExecutionCount,
                                    QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return new QuerySpanImpl(sdkBinding.getSdkBridge(), type, dest, text, queryMessageSupplier,
                System.currentTimeMillis());
    }

    @Override
    public AsyncQuerySpan startAsyncQuerySpan(String type, String dest, String text,
                                              QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return new AsyncQuerySpanImpl(sdkBinding.getSdkBridge(), type, dest, text, queryMessageSupplier,
                System.currentTimeMillis());
    }

    @Override
    public <C> Span startOutgoingSpan(String type, String text, Setter<C> setter, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName) {
        SdkBridge sdkBridge = sdkBinding.getSdkBridge();
        String outgoingSpanId = sdkBridge.propagate(new SdkBridge.Setter<>(setter), carrier,
                Global.isOutboundW3CEnabled(), Global.isOutboundW3CBackCompatEnabled());
        return new OutgoingSpanImpl(sdkBridge, type, text, System.currentTimeMillis(), outgoingSpanId,
                messageSupplier);
    }

    @Override
    public <C> AsyncSpan startAsyncOutgoingSpan(String type, String text, Setter<C> setter, C carrier,
                                                MessageSupplier messageSupplier, TimerName timerName) {
        SdkBridge sdkBridge = sdkBinding.getSdkBridge();
        String outgoingSpanId = sdkBridge.propagate(new SdkBridge.Setter<>(setter), carrier,
                Global.isOutboundW3CEnabled(), Global.isOutboundW3CBackCompatEnabled());
        return new AsyncOutgoingSpanImpl(sdkBridge, type, text, System.currentTimeMillis(), outgoingSpanId,
                messageSupplier);
    }

    @Override
    public void captureLoggerSpan(MessageSupplier messageSupplier, @Nullable Throwable throwable) {
        LoggerSpans.track(sdkBinding.getSdkBridge(), messageSupplier, throwable, System.currentTimeMillis());
    }

    @Override
    public Timer startTimer(TimerName timerName) {
        // timers are not used by ApplicationInsights
        return NopTransactionService.TIMER;
    }

    @Override
    public AuxThreadContext createAuxThreadContext() {
        return new AuxThreadContextImpl<>(sdkBinding, servletRequestInfo);
    }

    @Override
    public void setTransactionAsync() {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void setTransactionAsyncComplete() {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void setTransactionType(@Nullable String transactionType, int priority) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void setTransactionName(@Nullable String transactionName, int priority) {
        // currently ignoring priority, which is ok since just using core instrumentation
        if (transactionName != null) {
            sdkBinding.setOperationName(transactionName);
        }
    }

    @Override
    public void setTransactionUser(@Nullable String user, int priority) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void addTransactionAttribute(String name, @Nullable String value) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void setTransactionSlowThreshold(long threshold, TimeUnit unit, int priority) {
        // the core instrumentation only calls this to set slow threshold to zero for Startup spans
    }

    @Override
    public void setTransactionError(Throwable t) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void setTransactionError(@Nullable String message) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void setTransactionError(@Nullable String message, @Nullable Throwable t) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void trackResourceAcquired(Object resource, boolean withLocationStackTrace) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public void trackResourceReleased(Object resource) {
        // in SDK mode the incomingSpan level attributes are managed by the SDK
    }

    @Override
    public @Nullable ServletRequestInfo getServletRequestInfo() {
        return servletRequestInfo;
    }

    @Override
    public void setServletRequestInfo(ServletRequestInfo servletRequestInfo) {
        // not using servlet instrumentation, so this is never called
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
}
