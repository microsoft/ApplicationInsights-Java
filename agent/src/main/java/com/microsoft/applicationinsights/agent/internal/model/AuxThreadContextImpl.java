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

import com.microsoft.applicationinsights.agent.internal.bridge.SdkBridge;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.AuxThreadContext;
import org.glowroot.instrumentation.api.Span;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.instrumentation.engine.impl.NopTransactionService;

import static com.google.common.base.Preconditions.checkNotNull;

class AuxThreadContextImpl implements AuxThreadContext {

    private final @Nullable Object requestTelemetryContext;

    public AuxThreadContextImpl(@Nullable Object requestTelemetryContext) {
        this.requestTelemetryContext = requestTelemetryContext;
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
        threadContext = new ThreadContextImpl(0, 0);
        threadContextHolder.set(threadContext);
        if (completeAsyncTransaction) {
            threadContext.setTransactionAsyncComplete();
        }
        // guaranteed to have telemetry client at this point (see check in AgentImpl.startIncomingSpan())
        SdkBridge sdkBridge = checkNotNull(Global.getSdkBridge());
        sdkBridge.setRequestTelemetryContext(requestTelemetryContext);
        return new ThreadRootSpanImpl(threadContextHolder);
    }

}
