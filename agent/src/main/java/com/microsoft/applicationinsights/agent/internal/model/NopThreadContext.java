package com.microsoft.applicationinsights.agent.internal.model;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.*;

import java.util.concurrent.TimeUnit;

public class NopThreadContext implements ThreadContextPlus {

    private @Nullable ServletRequestInfo servletRequestInfo;

    private int currentNestingGroupId;
    private int currentSuppressionKeyId;

    public NopThreadContext(@Nullable ServletRequestInfo servletRequestInfo, int currentNestingGroupId,
                            int currentSuppressionKeyId) {
        this.servletRequestInfo = servletRequestInfo;
        this.currentNestingGroupId = currentNestingGroupId;
        this.currentSuppressionKeyId = currentSuppressionKeyId;
    }

    @Override
    public boolean isInTransaction() {
        return true;
    }

    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName, Getter<C> extractor, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName,
                                      AlreadyInTransactionBehavior alreadyInTransactionBehavior) {
        return NopTransactionService.LOCAL_SPAN;
    }

    @Override
    public Span startLocalSpan(MessageSupplier messageSupplier, TimerName timerName) {
        return NopTransactionService.LOCAL_SPAN;
    }

    @Override
    public QuerySpan startQuerySpan(String type, String dest, String text, QueryMessageSupplier queryMessageSupplier,
                                    TimerName timerName) {
        return NopTransactionService.QUERY_SPAN;
    }

    @Override
    public QuerySpan startQuerySpan(String type, String dest, String text, long queryExecutionCount,
                                    QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return NopTransactionService.QUERY_SPAN;
    }

    @Override
    public AsyncQuerySpan startAsyncQuerySpan(String type, String dest, String text,
                                              QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return NopTransactionService.ASYNC_QUERY_SPAN;
    }

    @Override
    public <C> Span startOutgoingSpan(String type, String text, Setter<C> setter, C carrier,
                                      MessageSupplier messageSupplier, TimerName timerName) {
        return NopTransactionService.LOCAL_SPAN;
    }

    @Override
    public <C> AsyncSpan startAsyncOutgoingSpan(String type, String text, Setter<C> setter, C carrier,
                                                MessageSupplier messageSupplier, TimerName timerName) {
        return NopTransactionService.ASYNC_SPAN;
    }

    @Override
    public void captureLoggerSpan(MessageSupplier messageSupplier, @Nullable Throwable throwable) {
    }

    @Override
    public Timer startTimer(TimerName timerName) {
        return NopTransactionService.TIMER;
    }

    @Override
    public AuxThreadContext createAuxThreadContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTransactionAsync() {
    }

    @Override
    public void setTransactionAsyncComplete() {
    }

    @Override
    public void setTransactionType(String transactionType, int priority) {
    }

    @Override
    public void setTransactionName(String transactionName, int priority) {
    }

    @Override
    public void setTransactionUser(String user, int priority) {
    }

    @Override
    public void addTransactionAttribute(String name, String value) {
    }

    @Override
    public void setTransactionSlowThreshold(long threshold, TimeUnit unit, int priority) {
    }

    @Override
    public void setTransactionError(Throwable t) {
    }

    @Override
    public void setTransactionError(String message) {
    }

    @Override
    public void setTransactionError(String message, Throwable t) {
    }

    @Override
    public void trackResourceAcquired(Object resource, boolean withLocationStackTrace) {
    }

    @Override
    public void trackResourceReleased(Object resource) {
    }

    @Override
    public @Nullable ServletRequestInfo getServletRequestInfo() {
        return servletRequestInfo;
    }

    @Override
    public void setServletRequestInfo(@Nullable ServletRequestInfo servletRequestInfo) {
        this.servletRequestInfo = servletRequestInfo;
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
