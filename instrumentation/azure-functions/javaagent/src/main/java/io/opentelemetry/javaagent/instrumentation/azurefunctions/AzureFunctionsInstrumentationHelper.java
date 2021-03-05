/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import io.opentelemetry.instrumentation.api.aisdk.AiConnectionString;
import io.opentelemetry.instrumentation.api.aisdk.AiWebsiteSiteName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureFunctionsInstrumentationHelper {

  private static final Logger logger =
      LoggerFactory.getLogger(AzureFunctionsInstrumentationHelper.class);

  public static void lazilySetConnectionStringAndWebsiteSiteName() {
    // race condition (two initial requests happening at the same time) is not a worry here
    // because at worst they both enter the condition below and update the connection string
    if (AiConnectionString.hasConnectionString() && AiWebsiteSiteName.hasWebsiteSiteName()) {
        return;
    }

    boolean lazySetOptIn = Boolean.parseBoolean(System.getProperty("LazySetOptIn"));
    String enableAgent = System.getenv("APPLICATIONINSIGHTS_ENABLE_AGENT");
    logger.info("lazySetOptIn: {}", lazySetOptIn);
    logger.info("APPLICATIONINSIGHTS_ENABLE_AGENT: {}", enableAgent);
    if (!shouldSetConnectionString(lazySetOptIn, enableAgent)) {
      return;
    }

    String connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");
    String instrumentationKey = System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY");
    setConnectionString(connectionString, instrumentationKey);

    String websiteSiteName = System.getenv("WEBSITE_SITE_NAME");
    setWebsiteSiteName(websiteSiteName);
  }

  static void setConnectionString(String connectionString, String instrumentationKey) {
    if (connectionString != null && !connectionString.isEmpty()) {
      AiConnectionString.setConnectionString(connectionString);
    } else {
      // if the instrumentation key is neither null nor empty , we will create a default
      // connection string based on the instrumentation key.
      // this is to support Azure Functions that were created prior to the introduction of
      // connection strings
      if (instrumentationKey != null && !instrumentationKey.isEmpty()) {
        AiConnectionString.setConnectionString("InstrumentationKey=" + instrumentationKey);
      }
    }
  }

  static void setWebsiteSiteName(String websiteSiteName) {
    if (websiteSiteName != null && !websiteSiteName.isEmpty()) {
      AiWebsiteSiteName.setWebsiteSiteName(websiteSiteName);
    }
  }

  static boolean shouldSetConnectionString(boolean lazySetOptIn, String enableAgent) {
    if (lazySetOptIn) {
      // when LazySetOptIn is on, enable agent if APPLICATIONINSIGHTS_ENABLE_AGENT is null or true
      if (enableAgent == null || Boolean.parseBoolean(enableAgent)) {
        return true;
      }
    } else {
      // when LazySetOptIn is off, enable agent if APPLICATIONINSIGHTS_ENABLE_AGENT is true
      if (Boolean.parseBoolean(enableAgent)) {
        return true;
      }
    }
    return false;
  }
}
