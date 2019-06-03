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

import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.*;

import static com.google.common.base.Preconditions.checkNotNull;

class AuxThreadContextImpl implements AuxThreadContext {

    private final IncomingSpanImpl incomingSpan;

    private final @Nullable RequestTelemetryContext telemetryContext;

    public AuxThreadContextImpl(IncomingSpanImpl incomingSpan, @Nullable RequestTelemetryContext telemetryContext) {
        this.incomingSpan = incomingSpan;
        this.telemetryContext = telemetryContext;
    }

    @Override
    public Span start() {
        return start(false);
    }

    @Override
    public Span startAndMarkAsyncTransactionComplete() {
        return start(true);
    }

    private Span start(boolean completeAsyncTransaction) {
        ThreadContextThreadLocal.Holder threadContextHolder = Global.getThreadContextHolder();
        ThreadContextPlus threadContext = threadContextHolder.get();
        if (threadContext != null) {
            if (completeAsyncTransaction) {
                threadContext.setTransactionAsyncComplete();
            }
            return NopTransactionService.LOCAL_SPAN;
        }
        threadContext = new ThreadContextImpl(incomingSpan, telemetryContext, 0, 0, true);
        threadContextHolder.set(threadContext);
        if (completeAsyncTransaction) {
            threadContext.setTransactionAsyncComplete();
        }
        ThreadContext.setRequestTelemetryContext(telemetryContext);
        return new AuxThreadSpanImpl(threadContextHolder);
    }

    private static class AuxThreadSpanImpl implements Span {

        private final ThreadContextThreadLocal.Holder threadContextHolder;

        private AuxThreadSpanImpl(ThreadContextThreadLocal.Holder threadContextHolder) {
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

        @Override
        @Deprecated
        public <R> void propagateToResponse(R response, Setter<R> setter) {
        }

        @Override
        @Deprecated
        public <R> void extractFromResponse(R response, Getter<R> getter) {
        }

        private void endInternal() {
            ThreadContextImpl threadContext = (ThreadContextImpl) checkNotNull(threadContextHolder.get());
            threadContextHolder.set(null);
            threadContext.endAuxThreadContext();
            // need to wait to clear thread local until after client.track() is called, since some telemetry
            // initializers
            // (e.g. WebOperationNameTelemetryInitializer) use it
            ThreadContext.setRequestTelemetryContext(null);
        }
    }
}
