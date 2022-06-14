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

import io.opentelemetry.contrib.attach.RuntimeAttach;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;

/** This class allows you to attach the Application Insights agent for Java at runtime. */
public class ApplicationInsights {

  private static final Logger LOGGER = Logger.getLogger(ApplicationInsights.class.getName());

  public static final String RUNTIME_ATTACHED_PROPERTY = "applicationinsights.internal.runtime.attached";

  private ApplicationInsights() {}

  /**
   * Attach the Application Insights agent for Java to the current JVM. The attachment must be
   * requested at the beginning of the main method.
   */
  public static void attach() {

    if (agentIsAttached()) {
      LOGGER.warning("Application Insights is already attached. It is not attached a second time.");
      return;
    }

    System.setProperty(RUNTIME_ATTACHED_PROPERTY, "true");

    Optional<File> optionalConfigFile = searchConfigFile();
    if (optionalConfigFile.isPresent()) {
      File configFile = optionalConfigFile.get();
      System.setProperty("applicationinsights.configuration.file", configFile.getAbsolutePath());
    }

    RuntimeAttach.attachJavaagentToCurrentJVM();
  }

  private static boolean agentIsAttached() {
    try {
      Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent", false, null);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static Optional<File> searchConfigFile() {

    URL jsonUrl = ApplicationInsights.class.getResource("/applicationinsights.json");

    boolean jsonFileNotFound = jsonUrl == null;
    if (jsonFileNotFound) {
      return Optional.empty();
    }

    return buildFileFrom(jsonUrl);
  }

  private static Optional<File> buildFileFrom(URL jsonConfigFileUrl) {
    try {
      URI jsonUri = jsonConfigFileUrl.toURI();
      File jsonFile = new File(jsonUri);
      return Optional.of(jsonFile);
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }
}
