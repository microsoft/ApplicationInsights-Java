// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.attach;

import io.opentelemetry.contrib.attach.core.CoreRuntimeAttach;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** This class allows you to attach the Application Insights agent for Java at runtime. */
public final class ApplicationInsights {

  /**
   * This property allows configuring an Application Insights json file. It can be helpful to get an
   * Application Insights json file by Spring profile.
   */
  public static final String APPLICATIONINSIGHTS_RUNTIME_ATTACH_CONFIGURATION_FILE =
      "applicationinsights.runtime-attach.configuration.classpath.file";

  private static final Logger logger = Logger.getLogger(ApplicationInsights.class.getName());

  private static final String RUNTIME_ATTACHED_ENABLED_PROPERTY =
      "applicationinsights.internal.runtime.attached";

  private static final String RUNTIME_ATTACHED_JSON_PROPERTY =
      "applicationinsights.internal.runtime.attached.json";

  private ApplicationInsights() {}

  /**
   * Attach the Application Insights agent for Java to the current JVM. The attachment must be
   * requested at the beginning of the main method.
   *
   * @throws ConfigurationException If the file given by the
   *     applicationinsights.runtime-attach.configuration.classpath.file property was not found
   */
  public static void attach() {

    if (agentIsAttached()) {
      logger.warning("Application Insights is already attached. It is not attached a second time.");
      return;
    }

    System.setProperty(RUNTIME_ATTACHED_ENABLED_PROPERTY, "true");

    try {
      // check from file system first so user can override the classpath file
      Optional<String> jsonConfig = findJsonConfigFromFileSystem();
      if (!jsonConfig.isPresent()) {
        jsonConfig = findJsonConfigFromClasspath();
      }
      if (jsonConfig.isPresent()) {
        System.setProperty(RUNTIME_ATTACHED_JSON_PROPERTY, jsonConfig.get());
      }

      String appInsightResourceName = findAppInsightResourceName();
      CoreRuntimeAttach runtimeAttach = new CoreRuntimeAttach(appInsightResourceName);

      runtimeAttach.attachJavaagentToCurrentJvm();
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Fail to runtime attach Application Insights", t);
    }
  }

  private static Optional<String> findJsonConfigFromClasspath() {

    String fileName = findJsonConfigFile();

    InputStream configContentAsInputStream = findResourceAsStream(fileName);

    if (configContentAsInputStream == null) {
      return Optional.empty();
    }

    String json = read(configContentAsInputStream);
    return Optional.of(json);
  }

  private static Optional<String> findJsonConfigFromFileSystem() {

    InputStream configContentAsInputStream = findJsonConfigFromFileSystemAsStream();

    if (configContentAsInputStream == null) {
      return Optional.empty();
    }

    String json = read(configContentAsInputStream);
    return Optional.of(json);
  }

  private static String read(InputStream configContentAsInputStream) {
    try (InputStreamReader inputStreamReader =
            new InputStreamReader(configContentAsInputStream, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
      return bufferedReader.lines().collect(Collectors.joining(""));
    } catch (IOException e) {
      throw new ConfigurationException(
          "Unexpected issue during loading of JSON configuration file: " + e.getMessage());
    }
  }

  @Nullable
  private static InputStream findResourceAsStream(String fileName) {
    InputStream configContentAsInputStream =
        ApplicationInsights.class.getResourceAsStream("/" + fileName);
    if (configContentAsInputStream == null && isJsonFileConfiguredWithProperty()) {
      throw new ConfigurationException(fileName + " not found on the class path");
    }
    return configContentAsInputStream;
  }

  @Nullable
  private static InputStream findJsonConfigFromFileSystemAsStream() {
    File defaultFile = new File("config/applicationinsights.json");
    if (!defaultFile.exists()) {
      defaultFile = new File("applicationinsights.json");
    }
    if (!defaultFile.exists()) {
      return null;
    }
    try {
      return Files.newInputStream(defaultFile.toPath());
    } catch (IOException e) {
      throw new ConfigurationException(
          "Unexpected issue during loading of JSON configuration file: " + e.getMessage());
    }
  }

  public static class ConfigurationException extends IllegalArgumentException {
    ConfigurationException(String message) {
      super(message);
    }
  }

  private static String findJsonConfigFile() {
    if (isJsonFileConfiguredWithProperty()) {
      return System.getProperty(APPLICATIONINSIGHTS_RUNTIME_ATTACH_CONFIGURATION_FILE);
    }
    return "applicationinsights.json";
  }

  private static boolean isJsonFileConfiguredWithProperty() {
    return System.getProperty(APPLICATIONINSIGHTS_RUNTIME_ATTACH_CONFIGURATION_FILE) != null;
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
