package com.microsoft.applicationinsights.agent.internal.utils;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;

// global state used instead of passing these to various classes (e.g. ThreadContextImpl, SpanImpl) in order
// to reduce memory footprint
public class Global {

    public static boolean isW3CEnabled;
    public static boolean isW3CBackportEnabled;

    public static @Nullable String cloudRole;

    private static final ThreadContextThreadLocal TCTL = new ThreadContextThreadLocal();

    private Global() {
    }

    public static ThreadContextThreadLocal getThreadContextThreadLocal() {
        return TCTL;
    }

    public static ThreadContextThreadLocal.Holder getThreadContextHolder() {
        return TCTL.getHolder();
    }

    // called via bytecode, see SpringApplicationClassFileTransformer
    public static void setCloudRole(@Nullable String cloudRole) {
        if (Global.cloudRole == null) {
            Global.cloudRole = cloudRole;
        }
    }

    public static @Nullable String getCloudRole() {
        return cloudRole;
    }
}
