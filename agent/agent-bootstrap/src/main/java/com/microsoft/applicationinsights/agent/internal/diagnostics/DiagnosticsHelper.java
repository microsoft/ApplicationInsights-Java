package com.microsoft.applicationinsights.agent.internal.diagnostics;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiagnosticsHelper {
    private DiagnosticsHelper() {
    }

    // visible for testing
    static volatile boolean appServiceCodeless;

    private static volatile boolean aksCodeless;

    private static volatile boolean functionsCodeless;

    // visible for testing
    static boolean enabled;

    // visible for testing
    static final String DIAGNOSTICS_OUTPUT_ENABLED_ENV_VAR_NAME = "APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_ENABLED";

    public static final String DIAGNOSTICS_LOGGER_NAME = "applicationinsights.diagnostics";

    static {
        boolean result = true;
        try {
            final String envValue = System.getenv(DIAGNOSTICS_OUTPUT_ENABLED_ENV_VAR_NAME);
            if (envValue != null) {
                // Boolean.parseBoolean will be false if string is not "true"; if var is garbage, assume enabled
                result = !envValue.equalsIgnoreCase("false");
            }
        } catch (Exception e) {
        }
        enabled = result;
    }

    public static void setAgentJarFile(File agentJarFile) {
        Path agentPath = agentJarFile.toPath();
        if (Files.exists(agentPath.resolveSibling("appsvc.codeless"))) {
            appServiceCodeless = true;
        } else if (Files.exists(agentPath.resolveSibling("aks.codeless"))) {
            aksCodeless = true;
        } else if (Files.exists(agentPath.resolveSibling("functions.codeless"))) {
            functionsCodeless = true;
        }
    }

    public static boolean isAppServiceCodeless() {
        return appServiceCodeless;
    }

    public static boolean isAksCodeless() {
        return aksCodeless;
    }

    public static boolean isFunctionsCodeless() {
        return functionsCodeless;
    }

    public static boolean isAnyCodelessAttach() {
        return appServiceCodeless || aksCodeless || functionsCodeless;
    }

    public static boolean shouldOutputDiagnostics() {
        return enabled && isAppServiceCodeless();
    }

}
