package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import java.nio.file.Files;
import java.nio.file.Path;

public class DiagnosticsHelper {
    private DiagnosticsHelper() { }

    /**
     * Values: true|false
     * Default: true
     */
    public static final String IPA_LOG_FILE_ENABLED_ENV_VAR = "APPLICATIONINSIGHTS_EXTENSION_LOG_FILE_ENABLED";

    /**
     * Default: "" (meaning diagnostics file output is disabled)
     */
    public static final String INTERNAL_LOG_OUTPUT_DIR_ENV_VAR = "APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_DIRECTORY";

    /**
     * Windows only. Cannot be enabled on non-windows OS.
     * Values: true|false
     * Default: true
     */
	public static final String IPA_ETW_PROVIDER_ENABLED_ENV_VAR = "APPLICATIONINSIGHTS_EXTENSION_ETW_PROVIDER_ENABLED";

    // visible for testing
    static volatile boolean appServiceCodeless;

    private static volatile boolean aksCodeless;

    private static volatile boolean functionsCodeless;

    private static final boolean isWindows;

    public static final String DIAGNOSTICS_LOGGER_NAME = "applicationinsights.extension.diagnostics";

    private static final ApplicationMetadataFactory METADATA_FACTORY = new ApplicationMetadataFactory();

    public static final String MDC_PROP_OPERATION = "microsoft.ai.operationName";

    static {
        final String osName = System.getProperty("os.name");
        isWindows = osName != null && osName.startsWith("Windows");
    }

    public static void setAgentJarFile(Path agentPath) {
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

    public static ApplicationMetadataFactory getMetadataFactory() {
        return METADATA_FACTORY;
    }

	public static String getCodelessResourceType() {
        if (appServiceCodeless) {
            return "appsvc";
        } else if (aksCodeless) {
            return "aks";
        } else if (functionsCodeless) {
            return "functions";
        }
        return null;
	}

	public static boolean isOsWindows() {
        return isWindows;
    }

}
