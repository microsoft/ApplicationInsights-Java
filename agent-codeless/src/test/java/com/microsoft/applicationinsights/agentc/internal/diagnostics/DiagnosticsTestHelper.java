package com.microsoft.applicationinsights.agentc.internal.diagnostics;

public class DiagnosticsTestHelper {
    private DiagnosticsTestHelper(){}
    public static void setIsAppService(boolean appService) {
        DiagnosticsHelper.appService = appService;
    }
}
