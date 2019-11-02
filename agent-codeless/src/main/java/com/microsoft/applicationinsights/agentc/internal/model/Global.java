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

// global state used instead of passing these to various classes (e.g. ThreadContextImpl, SpanImpl) in order
// to reduce memory footprint
public class Global {

    private static boolean distributedTracingOutboundEnabled;
    private static boolean distributedTracingRequestIdCompatEnabled;

    @Nullable
    private static volatile TelemetryClient telemetryClient;

    private static final ThreadContextThreadLocal TCTL = new ThreadContextThreadLocal();

    private Global() {
    }

    public static boolean isDistributedTracingOutboundEnabled() {
        return distributedTracingOutboundEnabled;
    }

    public static void setDistributedTracingOutboundEnabled(boolean distributedTracingOutboundEnabled) {
        Global.distributedTracingOutboundEnabled = distributedTracingOutboundEnabled;
    }

    public static boolean isDistributedTracingRequestIdCompatEnabled() {
        return distributedTracingRequestIdCompatEnabled;
    }

    public static void setDistributedTracingRequestIdCompatEnabled(boolean distributedTracingRequestIdCompatEnabled) {
        Global.distributedTracingRequestIdCompatEnabled = distributedTracingRequestIdCompatEnabled;
    }

    // this can be null if agent failed during startup
    @Nullable
    public static TelemetryClient getTelemetryClient() {
        return telemetryClient;
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
