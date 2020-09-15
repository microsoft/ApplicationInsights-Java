package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

public class DiagnosticsTestHelper {
    private DiagnosticsTestHelper() {
    }

    public static void setIsAppServiceCodeless(boolean appServiceCodeless) {
        DiagnosticsHelper.appServiceCodeless = appServiceCodeless;
    }

    public static void reset() {
        setIsAppServiceCodeless(false);
    }

}
