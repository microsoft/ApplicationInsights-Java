// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import io.opentelemetry.javaagent.bootstrap.servlet.ExperimentalSnippetHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// preview Configuration
public class SnippetConfiguration {
  private static final String snippet = readSnippet();
  private static final Logger LOGGER = LoggerFactory.getLogger(SnippetConfiguration.class);

  // visible for testing
  static String readSnippet() {
    try {
      Path path = getSnippetFilePath("javascript-snippet.txt");
      byte[] bytes = Files.readAllBytes(path);
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException | URISyntaxException e) {
      LOGGER.error("Failed to read javascript-snippet file", e);
    }
    return "";
  }

  private static Path getSnippetFilePath(String resourceName) throws URISyntaxException {
    ClassLoader classLoader = SnippetConfiguration.class.getClassLoader();
    return Paths.get(classLoader.getResource(resourceName).toURI());
  }

  public static void initializeSnippet(String connectionString) {
    ExperimentalSnippetHolder.setSnippet(
        snippet.replace("YOUR_CONNECTION_STRING", connectionString));
  }

  private SnippetConfiguration() {}
}
