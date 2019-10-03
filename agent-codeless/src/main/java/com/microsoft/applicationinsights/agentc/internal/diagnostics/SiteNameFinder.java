package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

public class SiteNameFinder extends CachedDiagnosticsValueFinder {
    @VisibleForTesting
    static final String WEBSITE_SITE_NAME_ENV_VAR = "WEBSITE_SITE_NAME";
    @VisibleForTesting
    static final String SITE_NAME_FIELD_NAME = "siteName";

    @Override
    public String getName() {
        return SITE_NAME_FIELD_NAME;
    }

    @Override
    protected String populateValue() {
        String fromEnv = System.getenv(SiteNameFinder.WEBSITE_SITE_NAME_ENV_VAR);
        if (Strings.isNullOrEmpty(fromEnv)) {
            return null;
        }
        return fromEnv;
    }
}
