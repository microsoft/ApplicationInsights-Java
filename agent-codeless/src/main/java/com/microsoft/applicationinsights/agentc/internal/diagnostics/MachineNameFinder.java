package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MachineNameFinder extends CachedDiagnosticsValueFinder {
    public static final String PROPERTY_NAME = "MachineName";

    @Nullable
    @Override
    protected String populateValue() {
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName != null) {
            return computerName;
        }
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null) {
            return hostname;
        }
        return null;
    }

    @Nonnull
    @Override
    public String getName() {
        return PROPERTY_NAME;
    }
}
