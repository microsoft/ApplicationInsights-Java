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

package com.microsoft.applicationinsights.agent.internal.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import org.apache.commons.text.lookup.StringLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileStringLookup implements StringLookup {

  private static final Logger logger = LoggerFactory.getLogger(FileStringLookup.class);
  private static final String PREFIX = "${file:";
  static final FileStringLookup INSTANCE = new FileStringLookup();

  @Override
  @Nullable
  public String lookup(final String key) {
    if (key == null || !key.startsWith(PREFIX)) {
      return null;
    }

    // treat it as valid json when '}' is missing from "${file:file.txt"
    int end = key.length();
    if (key.endsWith("}")) {
      --end;
    }

    String filePath = key.substring(PREFIX.length(), end);
    try {
      return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error(
          "I/O error occurs when loading connection string from the file '{}' with UTF-8 encoding.",
          filePath,
          e);
      return null;
    } catch (InvalidPathException e) {
      logger.error("Invalid file path for the connection string '{}'", filePath, e);
      return null;
    }
  }
}
