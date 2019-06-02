package com.microsoft.applicationinsights.agent.internal.utils;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.context.CloudContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

// global state used instead of passing these to various classes (e.g. ThreadContextImpl, SpanImpl) in order
// to reduce memory footprint
public class Global {

    public static boolean isW3CEnabled;
    public static boolean isW3CBackportEnabled;

    private static @Nullable TelemetryClient telemetryClient;

    // priority
    // * configured tag
    // * WEBSITE_SITE_NAME
    // * spring.application.name
    // * servlet context root
    private static @Nullable String cloudRole;

    private static final ThreadContextThreadLocal TCTL = new ThreadContextThreadLocal();

    // e.g. the one used by metrics, and user created TelemetryClients
    // this is using map/set that is not thread safe, so must be synchronized appropriately below
    private static final Set<TelemetryContext> otherTelemetryContexts =
            Collections.newSetFromMap(new WeakHashMap<TelemetryContext, Boolean>());

    private Global() {
    }

    public static TelemetryClient getTelemetryClient() {
        if (telemetryClient == null) {
            throw new IllegalStateException("Global.telemetryClient access too early");
        }
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

    // called via bytecode, see SpringApplicationClassFileTransformer
    public static void setCloudRole(@Nullable String cloudRole) {
        if (cloudRole == null) {
            return;
        }
        Global.cloudRole = cloudRole;
        setCloudRole(telemetryClient.getContext().getCloud());
        synchronized (otherTelemetryContexts) {
            for (TelemetryContext context : otherTelemetryContexts) {
                setCloudRole(context.getCloud());
            }
        }
    }

    private static void setCloudRole(CloudContext cloudContext) {
        if (cloudContext.getRole() == null) {
            cloudContext.setRole(cloudRole);
        }
    }

    public static class CloudRoleContextInitializer implements ContextInitializer {

        @Override
        public void initialize(TelemetryContext context) {
            synchronized (otherTelemetryContexts) {
                otherTelemetryContexts.add(context);
            }
            setCloudRole(context.getCloud());
        }
    }
}
