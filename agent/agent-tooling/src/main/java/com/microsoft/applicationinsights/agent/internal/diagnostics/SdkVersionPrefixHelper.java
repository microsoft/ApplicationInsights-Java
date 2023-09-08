// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import com.azure.monitor.opentelemetry.exporter.implementation.statsbeat.MetadataInstanceResponse;
import com.azure.monitor.opentelemetry.exporter.implementation.statsbeat.RpAttachType;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.PropertyHelper;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.SystemInformation;
import org.slf4j.LoggerFactory;

public final class SdkVersionPrefixHelper {

  private static volatile char rpIntegrationChar;

  /** Is resource provider (Azure Spring Cloud, AppService, Azure Functions, AKS, VM...). */
  public static boolean isRpIntegration() {
    return rpIntegrationChar != 0;
  }

  public static void setRpIntegrationChar(char ch) {
    rpIntegrationChar = ch;
  }

  public static void lazyUpdateVmRpIntegration(MetadataInstanceResponse response) {
    rpIntegrationChar = 'v';
    PropertyHelper.setSdkNamePrefix(SdkVersionPrefixHelper.getRpIntegrationSdkNamePrefix());
    RpAttachType.setRpAttachType(RpAttachType.STANDALONE_AUTO);
  }

  public static String getRpIntegrationSdkNamePrefix() {
    StringBuilder sdkNamePrefix = new StringBuilder(3);
    sdkNamePrefix.append(rpIntegrationChar);
    if (SystemInformation.isWindows()) {
      sdkNamePrefix.append("w");
    } else if (SystemInformation.isLinux()) {
      sdkNamePrefix.append("l");
    } else {
      LoggerFactory.getLogger("com.microsoft.applicationinsights.agent")
          .warn("could not detect os: {}", System.getProperty("os.name"));
      sdkNamePrefix.append("u");
    }
    sdkNamePrefix.append("_");
    return sdkNamePrefix.toString();
  }

  private SdkVersionPrefixHelper() {}
}
