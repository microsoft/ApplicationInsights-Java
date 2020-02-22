package com.microsoft.applicationinsights.agent.internal.diagnostics;

public class DiagnosticsTestHelper {
    private DiagnosticsTestHelper() {
    }

    public static final String ENABLED_ENV_VAR = DiagnosticsHelper.DIAGNOSTICS_OUTPUT_ENABLED_ENV_VAR_NAME;

    public static void setIsAppServiceCodeless(boolean appServiceCodeless) {
        DiagnosticsHelper.appServiceCodeless = appServiceCodeless;
    }

    public static void reset() {
        DiagnosticsHelper.enabled = true;
        setIsAppServiceCodeless(false);
    }

    public static void setEnabled(boolean enabled) {
        DiagnosticsHelper.enabled = enabled;
    }
}
