package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AgentExtensionVersionFinder extends CachedDiagnosticsValueFinder {

    /**
     * Follows variable naming scheme for extension versions: https://github
     * .com/projectkudu/kudu/wiki/Azure-Site-Extensions#pre-installed-site-extensions
     */
    public static final String AGENT_EXTENSION_VERSION_ENVIRONMENT_VARIABLE =
            "ApplicationInsightsAgent_EXTENSION_VERSION";

    @Nullable
    @Override
    protected String populateValue() {
        return System.getenv(AGENT_EXTENSION_VERSION_ENVIRONMENT_VARIABLE);
    }

    @Nonnull
    @Override
    public String getName() {
        return "extensionVersion";
    }
}
