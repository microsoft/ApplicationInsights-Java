package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import java.io.File;
import java.nio.file.Files;

import com.google.common.annotations.VisibleForTesting;

public class DiagnosticsHelper {
    private DiagnosticsHelper() {}

    private static File agentJarFile;

    @VisibleForTesting
    static volatile boolean appServiceCodeless;

    @VisibleForTesting
    static boolean enabled;

    @VisibleForTesting
    static final String DIAGNOSTICS_OUTPUT_ENABLED_ENV_VAR_NAME = "APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_ENABLED";

    static {
        boolean result = true;
        try {
            final String envValue = System.getenv(DIAGNOSTICS_OUTPUT_ENABLED_ENV_VAR_NAME);
            if (envValue != null) {
                result = !envValue.equalsIgnoreCase("false"); // Boolean.parseBoolean will be false if string is not "true"; if var is garbage, assume enabled
            }
        } catch (Exception e) {}
        enabled = result;
    }

    public static synchronized void setAgentJarFile(File agentJarFile) {
        DiagnosticsHelper.agentJarFile = agentJarFile;
        appServiceCodeless = Files.exists(agentJarFile.toPath().resolveSibling("appsvc.codeless"));
    }

    public static synchronized boolean isAppServiceCodeless() {
        return appServiceCodeless;
    }

    public static boolean shouldOutputDiagnostics() {
        return enabled && isAppServiceCodeless();
    }

}
