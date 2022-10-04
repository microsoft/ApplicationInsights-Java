// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.attach;

import io.opentelemetry.contrib.attach.CoreRuntimeAttach;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** This class allows you to attach the Application Insights agent for Java at runtime. */
public final class ApplicationInsights {

  private static final String APPLICATIONINSIGHTS_RUNTIME_ATTACH_CONFIGURATION_FILE =
      "applicationinsights.runtime-attach.configuration.file";

  private static final Logger logger = Logger.getLogger(ApplicationInsights.class.getName());

  private static final String RUNTIME_ATTACHED_ENABLED_PROPERTY =
      "applicationinsights.internal.runtime.attached";

  private static final String RUNTIME_ATTACHED_JSON_PROPERTY =
      "applicationinsights.internal.runtime.attached.json";

  private ApplicationInsights() {}

  /**
   * Attach the Application Insights agent for Java to the current JVM. The attachment must be
   * requested at the beginning of the main method.
   */
  public static void attach() {

    if (agentIsAttached()) {
      logger.warning("Application Insights is already attached. It is not attached a second time.");
      return;
    }

    System.setProperty(RUNTIME_ATTACHED_ENABLED_PROPERTY, "true");

    Optional<String> jsonConfig = findJsonConfig();
    if (jsonConfig.isPresent()) {
      System.setProperty(RUNTIME_ATTACHED_JSON_PROPERTY, jsonConfig.get());
    }

    String appInsightResourceName = findAppInsightResourceName();
    CoreRuntimeAttach runtimeAttach = new CoreRuntimeAttach(appInsightResourceName);

    runtimeAttach.attachJavaagentToCurrentJVM();
  }

  private static Optional<String> findJsonConfig() {

    String fileName = findJsonConfigFile();
    InputStream configContentAsInputStream =
        ApplicationInsights.class.getResourceAsStream("/" + fileName);
    if (configContentAsInputStream == null) {
      return Optional.empty();
    }
    try (InputStreamReader inputStreamReader =
            new InputStreamReader(configContentAsInputStream, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
      String json = bufferedReader.lines().collect(Collectors.joining(""));
      return Optional.of(json);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unexpected issue during loading of JSON configuration file: " + e.getMessage());
    }
  }

  private static String findJsonConfigFile() {
    String fileFromProperty =
        System.getProperty(APPLICATIONINSIGHTS_RUNTIME_ATTACH_CONFIGURATION_FILE);
    if (fileFromProperty != null) {
      return fileFromProperty;
    }
    return "applicationinsights.json";
  }

  private static boolean agentIsAttached() {
    try {
      Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent", false, null);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static String findAppInsightResourceName() {
    String appInsightVersion = findAppInsightVersion();
    return "/applicationinsights-agent-" + appInsightVersion + ".jar";
  }

  private static String findAppInsightVersion() {
    try (InputStream jarAsInputStream =
        ApplicationInsights.class.getResourceAsStream("/ai.sdk-version.properties")) {
      Properties props = new Properties();
      props.load(jarAsInputStream);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to find Application Insights version", e);
    }
  }
}
