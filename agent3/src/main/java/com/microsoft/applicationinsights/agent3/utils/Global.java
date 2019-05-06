package com.microsoft.applicationinsights.agent3.utils;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;

// global state used instead of passing these to various classes (e.g. ThreadContextImpl, SpanImpl) in order
// to reduce memory footprint
public class Global {

    // FIXME these need to be set via configuration (e.g. DefaultClassDataProvider.setConfiguration())
    public static boolean isW3CEnabled;
    public static boolean isW3CBackportEnabled;

    private Global() {
    }

    private static final ThreadContextThreadLocal TCTL = new ThreadContextThreadLocal();

    public static ThreadContextThreadLocal getThreadContextThreadLocal() {
        return TCTL;
    }

    public static ThreadContextThreadLocal.Holder getThreadContextHolder() {
        return TCTL.getHolder();
    }
}
