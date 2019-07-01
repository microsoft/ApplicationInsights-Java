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

import com.microsoft.applicationinsights.agent.internal.model.Global;
import com.microsoft.applicationinsights.agent.internal.model.ThreadContextImpl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.api.ThreadContext.ServletRequestInfo;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;

public class AgentBridgeInternal {

    private static final BindingResult NOP_BINDING_RESULT = new NopBindingResult();

    private AgentBridgeInternal() {
    }

    public static <T> BindingResult bindToThread(SdkBridge<T> sdkBridge, T requestTelemetryContext,
                                                 @Nullable ServletRequestInfo servletRequestInfo) {
        ThreadContextThreadLocal.Holder threadContextHolder = Global.getThreadContextHolder();
        ThreadContextPlus threadContext = threadContextHolder.get();
        if (threadContext == null) {
            SdkBinding<T> sdkBinding = new SdkBinding<>(sdkBridge, requestTelemetryContext);
            threadContextHolder.set(new ThreadContextImpl<>(sdkBinding, servletRequestInfo, 0, 0));
            return sdkBinding;
        } else {
            return NOP_BINDING_RESULT;
        }
    }

    private static class NopBindingResult implements BindingResult {

        @Override
        public void unbindFromMainThread() {
        }

        @Override
        public void unbindFromRunawayChildThreads() {
        }
    }
}
