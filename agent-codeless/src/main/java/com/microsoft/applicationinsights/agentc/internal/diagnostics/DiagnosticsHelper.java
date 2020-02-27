package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.annotations.VisibleForTesting;

public class DiagnosticsHelper {
    private DiagnosticsHelper() { }

    public static final String IPA_LOG_FILE_ENABLED_ENV_VAR = "APPLICATIONINSIGHTS_EXTENSION_LOG_FILE_ENABLED";

    public static final String INTERNAL_LOG_OUTPUT_DIR_ENV_VAR = "APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_DIRECTORY";

    @VisibleForTesting
    static volatile boolean appServiceCodeless;

    private static volatile boolean aksCodeless;

    private static volatile boolean functionsCodeless;

    public static final String DIAGNOSTICS_LOGGER_NAME = "applicationinsights.extension.diagnostics";

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

}
