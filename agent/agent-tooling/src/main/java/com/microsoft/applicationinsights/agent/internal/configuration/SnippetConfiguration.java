// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import io.opentelemetry.javaagent.bootstrap.servlet.ExperimentalSnippetHolder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// preview Configuration
public class SnippetConfiguration {
  private static String snippet = readSnippet();

  public static String readSnippet(){
    try {
      Path path = Paths.get("/resources/snippet.txt");
      byte[] bytes = Files.readAllBytes(path);
      return new String(bytes, Charset.defaultCharset());
    } catch(IOException e){
      throw new UncheckedIOException(e);
    }
  }

  public static void setSnippet(String connectionString) {
    System.out.println("------------------debug-------------" + snippet);
    snippet = snippet.replace("CONNECTION_STRING", connectionString);
    ExperimentalSnippetHolder.setSnippet(snippet);
  }

  private SnippetConfiguration() {}
}
