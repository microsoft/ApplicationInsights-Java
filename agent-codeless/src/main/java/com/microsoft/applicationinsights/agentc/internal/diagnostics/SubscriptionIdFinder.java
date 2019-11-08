package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Strings;

public class SubscriptionIdFinder extends CachedDiagnosticsValueFinder {

    private static final String WEBSITE_OWNER_NAME_ENV_VAR = "WEBSITE_OWNER_NAME";

    @Nullable
    @Override
    protected String populateValue() {
        final String envValue = System.getenv(WEBSITE_OWNER_NAME_ENV_VAR);
        if (Strings.isNullOrEmpty(envValue)) {
            return "unknown";
        }
        final int index = envValue.indexOf('+');
        if (index < 0) {
            return "unknown";
        }
        return envValue.substring(0, index);
    }

    @Nonnull
    @Override
    public String getName() {
        return "subscriptionId";
    }
}
