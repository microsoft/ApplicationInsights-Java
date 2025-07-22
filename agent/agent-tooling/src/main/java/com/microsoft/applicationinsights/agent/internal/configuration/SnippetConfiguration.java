// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import io.opentelemetry.javaagent.bootstrap.servlet.ExperimentalSnippetHolder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// preview Configuration
public class SnippetConfiguration {
  private static final String snippet = readSnippet();
  private static final Logger logger = LoggerFactory.getLogger(SnippetConfiguration.class);

  // visible for testing
  static String readSnippet() {
    ClassLoader classLoader = SnippetConfiguration.class.getClassLoader();
    String resourceName = "browser-sdk-loader-snippet.txt";
    InputStream inputStream = classLoader.getResourceAsStream(resourceName);
    if (inputStream == null) {
      logger.error("Resource not found: " + resourceName);
      return "";
    }
    try {
      return toString(inputStream);
    } catch (IOException e) {
      // Handle any IO exceptions that occur
      logger.error("Failed to read javascript-snippet file", e);
    }
    return "";
  }

  private static String toString(InputStream inputStream) throws IOException {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return bufferedReader.lines().collect(Collectors.joining("\n"));
    }
  }

  public static void initializeSnippet(String connectionString) {
    ExperimentalSnippetHolder.setSnippet(
        snippet.replace("YOUR_CONNECTION_STRING", connectionString));
  }

  private SnippetConfiguration() {}
}
