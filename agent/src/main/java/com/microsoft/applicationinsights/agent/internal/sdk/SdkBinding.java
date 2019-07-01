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

package com.microsoft.applicationinsights.agent.internal.sdk;

import java.util.Set;

import com.google.common.collect.Sets;
import com.microsoft.applicationinsights.agent.internal.model.Global;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;

public class SdkBinding<T> implements BindingResult {

    private final SdkBridge<T> sdkBridge;

    private final @Nullable T requestTelemetryContext;

    // aux thread contexts started by this sdk binding (request)
    private final Set<ThreadContextThreadLocal.Holder> auxThreadContextHolders = Sets.newHashSet();

    SdkBinding(SdkBridge<T> sdkBridge, @Nullable T requestTelemetryContext) {
        this.sdkBridge = sdkBridge;
        this.requestTelemetryContext = requestTelemetryContext;
    }

    public SdkBridge<T> getSdkBridge() {
        return sdkBridge;
    }

    public void bindRequestTelemetryContext() {
        if (requestTelemetryContext != null) {
            sdkBridge.bindRequestTelemetryContext(requestTelemetryContext);
        }
    }

    public void unbindRequestTelemetryContext() {
        sdkBridge.unbindRequestTelemetryContext();
    }

    public void setOperationName(String operationName) {
        if (requestTelemetryContext != null) {
            sdkBridge.setOperationName(requestTelemetryContext, operationName);
        }
    }

    public void addAuxThreadContextHolder(ThreadContextThreadLocal.Holder auxThreadContextHolder) {
        // since other accesses to auxThreadContextHolders are synchronized, may as well make this one synchronized and
        // then don't need to use a concurrent hash set
        synchronized (auxThreadContextHolders) {
            auxThreadContextHolders.add(auxThreadContextHolder);
        }
    }

    public void removeAuxThreadContextHolder(ThreadContextThreadLocal.Holder auxThreadContextHolder) {
        // this synchronized is to make sure it doesn't detach the holder after it has been picked up by another request
        synchronized (auxThreadContextHolders) {
            auxThreadContextHolders.remove(auxThreadContextHolder);
        }
    }

    @Override
    public void unbindFromMainThread() {
        Global.getThreadContextHolder().set(null);
    }

    @Override
    public void unbindFromRunawayChildThreads() {
        // this synchronized is to make sure it doesn't detach the holder after it has been picked up by another request
        synchronized (auxThreadContextHolders) {
            for (ThreadContextThreadLocal.Holder auxThreadContextHolder : auxThreadContextHolders) {
                auxThreadContextHolder.set(null);
            }
        }
    }
}
