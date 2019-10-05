package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class PidFinder extends CachedDiagnosticsValueFinder {
    public static final String PROPERTY_NAME = "PID";

    @Nullable
    @Override
    protected String populateValue() {
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

    @Nonnull
    @Override
    public String getName() {
        return PROPERTY_NAME;
    }
}
