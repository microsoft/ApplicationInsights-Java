package io.opentelemetry.instrumentation.auto.azure.functions;

import com.google.common.base.Strings;
import io.opentelemetry.instrumentation.api.aiconnectionstring.AiConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureFunctionsInstrumentationHelper {

  private static final Logger logger = LoggerFactory
      .getLogger(AzureFunctionsInstrumentationHelper.class);

  public static void lazilySetConnectionString() {
    // race condition (two initial requests happening at the same time) is not a worry here
    // because at worst they both enter the condition below and update the connection string
    if (AiConnectionString.hasConnectionString()) {
      return;
    }

    boolean lazySetOptIn = Boolean.parseBoolean(System.getProperty("LazySetOptIn"));
    String connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");
    String instrumentationKey = System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY");
    String enableAgent = System.getenv("APPLICATIONINSIGHTS_ENABLE_AGENT");
    logger.info("lazySetOptIn: {}", lazySetOptIn);
    logger.info("APPLICATIONINSIGHTS_ENABLE_AGENT: {}", enableAgent);

    if (!shouldSetConnectionString(lazySetOptIn, enableAgent)) {
      return;
    }

    setConnectionString(connectionString, instrumentationKey);
  }

  static void setConnectionString(String connectionString, String instrumentationKey) {
    if (!Strings.isNullOrEmpty(connectionString)) {
      AiConnectionString.setConnectionString(connectionString);
    } else {
      // if the instrumentation key is neither null nor empty , we will create a default
      // connection string based on the instrumentation key.
      // this is to support Azure Functions that were created prior to the introduction of
      // connection strings
      if (!Strings.isNullOrEmpty(instrumentationKey)) {
        AiConnectionString.setConnectionString("InstrumentationKey=" + instrumentationKey);
      }
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
