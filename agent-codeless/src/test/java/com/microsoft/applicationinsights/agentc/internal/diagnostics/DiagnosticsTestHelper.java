package com.microsoft.applicationinsights.agentc.internal.diagnostics;

public class DiagnosticsTestHelper {
    private DiagnosticsTestHelper(){}
    public static void setIsAppServiceCodeless(boolean appServiceCodeless) {
        DiagnosticsHelper.appServiceCodeless = appServiceCodeless;
    }
}
