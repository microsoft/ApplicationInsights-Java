package com.microsoft.applicationinsights.agent.internal.diagnostics;

public abstract class CachedDiagnosticsValueFinder implements DiagnosticsValueFinder {
    private volatile String value;

    @Override
    public String getValue() {
        if (value == null) {
            value = populateValue();
        }
        return value;
    }

    protected abstract String populateValue();
}
