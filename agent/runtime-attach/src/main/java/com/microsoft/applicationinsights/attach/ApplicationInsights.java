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

package com.microsoft.applicationinsights.attach;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** This class allows you to attach the Application Insights agent for Java at runtime. */
public final class ApplicationInsights {

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

    File agentFile = AppInsightAgentFileProvider.getAgentFile();

    try {
      RuntimeAttach.attachJavaagentToCurrentJvm(agentFile);
    } catch (IllegalStateException e) {
      if (e.getMessage()
          .contains("No compatible attachment provider is available")) { // Byte Buddy exception
        throw new IllegalStateException("Runtime attachment was not done. You may use a JRE.", e);
      }
      throw e;
    }
  }

  private static Optional<String> findJsonConfig() {

    InputStream configContentAsInputStream =
        ApplicationInsights.class.getResourceAsStream("/applicationinsights.json");
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

  private static boolean agentIsAttached() {
    try {
      Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent", false, null);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
