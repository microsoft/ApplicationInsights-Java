// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import com.azure.monitor.opentelemetry.exporter.implementation.statsbeat.RpAttachType;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.SdkVersionPrefixHolder;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiagnosticsHelper {
  private DiagnosticsHelper() {}

  // visible for testing
  public static volatile boolean appSvcRpIntegration;
  private static volatile boolean functionsRpIntegration;

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

  public static void initRpIntegration(Path agentPath) {
    // important to check FUNCTIONS_WORKER_RUNTIME before WEBSITE_SITE_NAME
    // TODO (heya) how should we report functions windows users who are using app services
    //  windows attach by manually setting the env vars (which was the old documented way)
    if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      SdkVersionPrefixHolder.setRpIntegrationChar('f');
      functionsRpIntegration = true;
      setRpAttachType(agentPath, "functions.codeless");
    } else if (!Strings.isNullOrEmpty(System.getenv("WEBSITE_SITE_NAME"))) {
      SdkVersionPrefixHolder.setRpIntegrationChar('a');
      appSvcRpIntegration = true;
      setRpAttachType(agentPath, "appsvc.codeless");
    } else if (!Strings.isNullOrEmpty(System.getenv("KUBERNETES_SERVICE_HOST"))) {
      SdkVersionPrefixHolder.setRpIntegrationChar('k');
      setRpAttachType(agentPath, "aks.codeless");
    } else if (!Strings.isNullOrEmpty(
        System.getenv("APPLICATIONINSIGHTS_SPRINGCLOUD_SERVICE_ID"))) {
      SdkVersionPrefixHolder.setRpIntegrationChar('s');
      setRpAttachType(agentPath, "springcloud.codeless");
    }
    // TODO (heya) detect VM environment by checking the AzureMetadataService response, manual only
  }

  public static boolean isAppSvcRpIntegration() {
    return appSvcRpIntegration;
  }

  public static boolean isFunctionsRpIntegration() {
    return functionsRpIntegration;
  }

  public static ApplicationMetadataFactory getMetadataFactory() {
    return METADATA_FACTORY;
  }

  public static boolean isOsWindows() {
    return isWindows;
  }

  private static void setRpAttachType(Path agentPath, String markerFile) {
    if (Files.exists(agentPath.resolveSibling(markerFile))) {
      RpAttachType.setRpAttachType(RpAttachType.INTEGRATED_AUTO);
    } else {
      RpAttachType.setRpAttachType(RpAttachType.STANDALONE_AUTO);
    }
  }
}
