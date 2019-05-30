package com.microsoft.applicationinsights.agent.internal.model;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.Timer;

public class NopThreadSpan implements Span {

    private final ThreadContextThreadLocal.Holder threadContextHolder;

    public NopThreadSpan(ThreadContextThreadLocal.Holder threadContextHolder) {
        this.threadContextHolder = threadContextHolder;
    }

    @Override
    public void end() {
        endInternal();
    }

    @Override
    public void endWithLocationStackTrace(long thresholdNanos) {
        endInternal();
    }

    @Override
    public void endWithError(Throwable t) {
        endInternal();
    }

    @Override
    public Timer extend() {
        // extend() shouldn't be called on auxiliary thread span
        return NopTransactionService.TIMER;
    }

    @Override
    public @Nullable Object getMessageSupplier() {
        return null;
    }

    private void endInternal() {
        threadContextHolder.set(null);
    }

    @Override
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {
    }

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {
    }
}
