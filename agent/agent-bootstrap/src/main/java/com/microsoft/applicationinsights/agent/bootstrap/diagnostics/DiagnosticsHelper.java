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
    static volatile boolean appSvcAttachForLoggingPurposes;

    private static volatile char attachChar;

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
            appSvcAttachForLoggingPurposes = true;
            if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
                attachChar = 'f';
            } else {
                attachChar = 'a';
            }
        } else if (Files.exists(agentPath.resolveSibling("aks.codeless"))) {
            attachChar = 'k';
        } else if (Files.exists(agentPath.resolveSibling("functions.codeless"))) {
            attachChar = 'f';
        } else if (Files.exists(agentPath.resolveSibling("springcloud.codeless"))) {
            attachChar = 's';
        }
    }

    public static boolean isAnyAttach() {
        return attachChar != 0;
    }

    // returns 0 if not attach
    public static char attachChar() {
        return attachChar;
    }

    // this also applies to Azure Functions running on App Services
    public static boolean isAppSvcAttachForLoggingPurposes() {
        return appSvcAttachForLoggingPurposes;
    }

    public static ApplicationMetadataFactory getMetadataFactory() {
        return METADATA_FACTORY;
    }

	public static boolean isOsWindows() {
        return isWindows;
    }
}
