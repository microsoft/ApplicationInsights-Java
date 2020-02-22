package com.microsoft.applicationinsights.agent.internal.diagnostics;

public class MachineNameFinder extends CachedDiagnosticsValueFinder {
    public static final String PROPERTY_NAME = "MachineName";

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

    @Override
    public String getName() {
        return PROPERTY_NAME;
    }
}
