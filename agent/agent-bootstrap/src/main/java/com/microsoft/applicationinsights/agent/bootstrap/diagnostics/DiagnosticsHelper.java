/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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

  public static final String DIAGNOSTICS_LOGGER_NAME = "applicationinsights.extension.diagnostics";

  private static final ApplicationMetadataFactory METADATA_FACTORY =
      new ApplicationMetadataFactory();

  public static final String MDC_PROP_OPERATION = "microsoft.ai.operationName";

  static {
    String osName = System.getProperty("os.name");
    isWindows = osName != null && osName.startsWith("Windows");
  }

  public static void setAgentJarFile(Path agentPath) {
    if (Files.exists(agentPath.resolveSibling("appsvc.codeless"))) {
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
