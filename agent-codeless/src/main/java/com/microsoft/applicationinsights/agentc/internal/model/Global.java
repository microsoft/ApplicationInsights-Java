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

import com.microsoft.applicationinsights.TelemetryClient;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;

import static com.google.common.base.Preconditions.checkNotNull;

// global state used instead of passing these to various classes (e.g. ThreadContextImpl, SpanImpl) in order
// to reduce memory footprint
public class Global {

    private static boolean outboundW3CEnabled;
    private static boolean outboundW3CBackCompatEnabled;

    // note: inboundW3CBackCompatEnabled is stored in TraceContextCorrelationCore
    private static boolean inboundW3CEnabled;

    private static volatile @Nullable TelemetryClient telemetryClient;

    private static final ThreadContextThreadLocal TCTL = new ThreadContextThreadLocal();

    private Global() {
    }

    public static boolean isOutboundW3CEnabled() {
        return outboundW3CEnabled;
    }

    public static void setOutboundW3CEnabled(boolean outboundW3CEnabled) {
        Global.outboundW3CEnabled = outboundW3CEnabled;
    }

    public static boolean isOutboundW3CBackCompatEnabled() {
        return outboundW3CBackCompatEnabled;
    }

    public static void setOutboundW3CBackCompatEnabled(boolean outboundW3CBackCompatEnabled) {
        Global.outboundW3CBackCompatEnabled = outboundW3CBackCompatEnabled;
    }

    public static boolean isInboundW3CEnabled() {
        return inboundW3CEnabled;
    }

    public static void setInboundW3CEnabled(boolean inboundW3CEnabled) {
        Global.inboundW3CEnabled = inboundW3CEnabled;
    }

    public static TelemetryClient getTelemetryClient() {
        return checkNotNull(telemetryClient);
    }

    public static void setTelemetryClient(TelemetryClient telemetryClient) {
        Global.telemetryClient = telemetryClient;
    }

    public static ThreadContextThreadLocal getThreadContextThreadLocal() {
        return TCTL;
    }

    public static ThreadContextThreadLocal.Holder getThreadContextHolder() {
        return TCTL.getHolder();
    }
}
