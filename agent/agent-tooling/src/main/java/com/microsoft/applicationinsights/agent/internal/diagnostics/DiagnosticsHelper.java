// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.statsbeat.RpAttachType;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.PropertyHelper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.Strings;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiagnosticsHelper {
  private DiagnosticsHelper() {}

  // visible for testing
  private static volatile boolean appSvcRpIntegratedAuto;
  private static volatile boolean functionsRpIntegratedAuto;

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
    // TODO (heya) refactor PropertyHelper and ResourceProvider to simplify logic for rp
    if ("java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      PropertyHelper.setRpIntegrationChar('f');
      setRpAttachType(agentPath, "functions.codeless");
      if (RpAttachType.INTEGRATED_AUTO.equals(RpAttachType.getRpAttachType())) {
        functionsRpIntegratedAuto = true;
      }
    } else if (!Strings.isNullOrEmpty(System.getenv("WEBSITE_SITE_NAME"))) {
      PropertyHelper.setRpIntegrationChar('a');
      setRpAttachType(agentPath, "appsvc.codeless");
      if (RpAttachType.INTEGRATED_AUTO.equals(RpAttachType.getRpAttachType())) {
        appSvcRpIntegratedAuto = true;
      }
    } else if (!Strings.isNullOrEmpty(
        System.getenv("APPLICATIONINSIGHTS_SPRINGCLOUD_SERVICE_ID"))) {
      // Spring Cloud needs to be checked before AKS since it runs on AKS
      PropertyHelper.setRpIntegrationChar('s');
      setRpAttachType(agentPath, "springcloud.codeless");
    } else if (!Strings.isNullOrEmpty(System.getenv("AKS_ARM_NAMESPACE_ID"))) {
      // AKS_ARM_NAMESPACE_ID is an env var available in AKS only and it's also used as the AKS
      // attach rate numerator
      PropertyHelper.setRpIntegrationChar('k');
      setRpAttachType(agentPath, "aks.codeless");
    } else {
      RpAttachType.setRpAttachType(RpAttachType.STANDALONE_AUTO); // default
    }
  }

  public static boolean isAppSvcRpIntegratedAuto() {
    return appSvcRpIntegratedAuto;
  }

  public static boolean isFunctionsRpIntegratedAuto() {
    return functionsRpIntegratedAuto;
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

  // only used by tests
  public static void setAppSvcRpIntegratedAuto(boolean appSvcRpIntegratedAuto) {
    DiagnosticsHelper.appSvcRpIntegratedAuto = appSvcRpIntegratedAuto;
  }
}
