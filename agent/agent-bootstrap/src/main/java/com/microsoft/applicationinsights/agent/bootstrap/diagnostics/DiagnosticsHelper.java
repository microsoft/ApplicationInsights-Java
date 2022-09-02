// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import java.nio.file.Files;
import java.nio.file.Path;

public class DiagnosticsHelper {
  private DiagnosticsHelper() {}

  // Default is "" (meaning diagnostics file output is disabled)
  public static final String APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_DIRECTORY =
      "APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_DIRECTORY";

  // visible for testing
  static volatile boolean useAppSvcRpIntegrationLogging;
  private static volatile boolean useFunctionsRpIntegrationLogging;

  private static volatile char rpIntegrationChar;

  private static final boolean isWindows;

  public static final String LINUX_DEFAULT = "/var/log/applicationinsights";
  public static final String DIAGNOSTICS_LOGGER_NAME = "applicationinsights.extension.diagnostics";

  private static final ApplicationMetadataFactory METADATA_FACTORY =
      new ApplicationMetadataFactory();

  public static final String MDC_MESSAGE_ID = "msgId";
  public static final String MDC_PROP_OPERATION = "microsoft.ai.operationName";

  static {
    String osName = System.getProperty("os.name");
    isWindows = osName != null && osName.startsWith("Windows");
  }

  public static void setAgentJarFile(Path agentPath) {
    if (Files.exists(agentPath.resolveSibling("appsvc.codeless"))) {
      // TODO we can remove this check after the new functions model is deployed.
      if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
        rpIntegrationChar = 'f';
      } else {
        rpIntegrationChar = 'a';
      }
      useAppSvcRpIntegrationLogging = true;
    } else if (Files.exists(agentPath.resolveSibling("aks.codeless"))) {
      rpIntegrationChar = 'k';
    } else if (Files.exists(agentPath.resolveSibling("functions.codeless"))) {
      rpIntegrationChar = 'f';
      useFunctionsRpIntegrationLogging = true;
    } else if (Files.exists(agentPath.resolveSibling("springcloud.codeless"))) {
      rpIntegrationChar = 's';
    }
  }

  /** Is resource provider (Azure Spring Cloud, AppService, Azure Functions, AKS, VM...). */
  public static boolean isRpIntegration() {
    return rpIntegrationChar != 0;
  }

  // returns 0 if not rp integration
  public static char rpIntegrationChar() {
    return rpIntegrationChar;
  }

  // this also applies to Azure Functions running on App Services
  public static boolean useAppSvcRpIntegrationLogging() {
    return useAppSvcRpIntegrationLogging;
  }

  // this also applies to Azure Functions running on App Services
  public static boolean useFunctionsRpIntegrationLogging() {
    return useFunctionsRpIntegrationLogging;
  }

  public static ApplicationMetadataFactory getMetadataFactory() {
    return METADATA_FACTORY;
  }

  public static boolean isOsWindows() {
    return isWindows;
  }
}
