package com.microsoft.applicationinsights.agent.internal.diagnostics;

public class SiteNameFinder extends CachedDiagnosticsValueFinder {
    // visible for testing
    static final String WEBSITE_SITE_NAME_ENV_VAR = "WEBSITE_SITE_NAME";
    // visible for testing
    static final String SITE_NAME_FIELD_NAME = "siteName";

    @Override
    public String getName() {
        return SITE_NAME_FIELD_NAME;
    }

    @Override
    protected String populateValue() {
        String value = System.getenv(SiteNameFinder.WEBSITE_SITE_NAME_ENV_VAR);
        return value == null || value.isEmpty() ? null : value;
    }
}
