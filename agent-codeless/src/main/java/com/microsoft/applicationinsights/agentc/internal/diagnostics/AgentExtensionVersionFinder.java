package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AgentExtensionVersionFinder extends CachedDiagnosticsValueFinder {
    @Nullable
    @Override
    protected String populateValue() {
        return System.getenv("ApplicationInsightsAgent_EXTENSION_VERSION");
    }

    @Nonnull
    @Override
    public String getName() {
        return "extensionVersion";
    }
}
