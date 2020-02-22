package com.microsoft.applicationinsights.agent.internal.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;

public class PidFinder extends CachedDiagnosticsValueFinder {
    public static final String PROPERTY_NAME = "PID";

    @Override
    protected String populateValue() {
        String java9pid = getPidUsingProcessHandle();
        if (java9pid != null) {
            return java9pid;
        }

        return getPidUsingRuntimeBean();
    }

    private String getPidUsingRuntimeBean() {
        // will only work with sun based jvm
        final RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        if (rb == null) {
            return null;
        }
        final String name = rb.getName();
        if (name == null) {
            return null;
        }
        final String pid = name.split("@")[0];
        if (pid == null) {
            return null;
        }
        try {
            return String.valueOf(Integer.parseInt(pid));
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private String getPidUsingProcessHandle() {
        try {
            // if java.specification.version < 9, the next line will fail.
            final Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            final Method currentProcessHandleMethod = processHandleClass.getMethod("current");
            Object currentProcessHandle = currentProcessHandleMethod.invoke(null);
            if (currentProcessHandle == null) {
                return null;
            }

            final Method pidMethod = processHandleClass.getMethod("pid");
            final Object pid = pidMethod.invoke(currentProcessHandle);
            if (pid == null) {
                return null;
            }
            return pid.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return PROPERTY_NAME;
    }
}
