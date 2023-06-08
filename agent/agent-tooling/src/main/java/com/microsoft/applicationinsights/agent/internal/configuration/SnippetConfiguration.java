// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import io.opentelemetry.javaagent.bootstrap.servlet.ExperimentalSnippetHolder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// preview Configuration
public class SnippetConfiguration {
  private static String snippet = readSnippet();

  static Path getConfigFilePath(String resourceName) throws URISyntaxException {
    ClassLoader classLoader = SnippetConfiguration.class.getClassLoader();
    return Paths.get(classLoader.getResource(resourceName).toURI());
  }
  public static String readSnippet(){
    try {
      Path path = getConfigFilePath("snippet.txt");
      byte[] bytes = Files.readAllBytes(path);
      return new String(bytes, Charset.defaultCharset());
    } catch(IOException | URISyntaxException e){
      throw new UncheckedIOException((IOException) e);
    }
  }

  public static void setSnippet(String connectionString) {
    snippet = snippet.replace("CONNECTION_STRING", connectionString);
    ExperimentalSnippetHolder.setSnippet(snippet);
  }

  private SnippetConfiguration() {}
}
