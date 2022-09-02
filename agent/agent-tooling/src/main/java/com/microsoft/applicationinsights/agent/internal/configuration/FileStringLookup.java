// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.apache.commons.text.lookup.StringLookup;

final class FileStringLookup implements StringLookup {

  private final Path baseDir;

  FileStringLookup(Path baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public String lookup(String key) {
    try {
      return new String(Files.readAllBytes(baseDir.resolve(key)), StandardCharsets.UTF_8);
    } catch (IOException | InvalidPathException e) {
      throw new IllegalArgumentException(
          "Error occurs when reading connection string from the file '"
              + key
              + "' with UTF-8 encoding.",
          e);
    }
  }
}
