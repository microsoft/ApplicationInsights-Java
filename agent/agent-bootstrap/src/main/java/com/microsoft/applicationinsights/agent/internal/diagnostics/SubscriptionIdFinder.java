package com.microsoft.applicationinsights.agent.internal.diagnostics;

public class SubscriptionIdFinder extends CachedDiagnosticsValueFinder {

    // visible for testing
    static final String WEBSITE_OWNER_NAME_ENV_VAR = "WEBSITE_OWNER_NAME";

    @Override
    protected String populateValue() {
        final String envValue = System.getenv(WEBSITE_OWNER_NAME_ENV_VAR);
        if (envValue == null || envValue.isEmpty()) {
            return "unknown";
        }
        final int index = envValue.indexOf('+');
        if (index < 0) {
            return "unknown";
        }
        return envValue.substring(0, index);
    }

    @Override
    public String getName() {
        return "subscriptionId";
    }
}
