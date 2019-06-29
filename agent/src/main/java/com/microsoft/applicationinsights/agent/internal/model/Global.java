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

import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;

// global state used instead of passing these to various classes (e.g. ThreadContextImpl, SpanImpl) in order
// to reduce memory footprint
public class Global {

    private static boolean outboundW3CEnabled;
    private static boolean outboundW3CBackCompatEnabled;

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

    public static ThreadContextThreadLocal getThreadContextThreadLocal() {
        return TCTL;
    }

    public static ThreadContextThreadLocal.Holder getThreadContextHolder() {
        return TCTL.getHolder();
    }
}
