// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.logbackpatch;

import ch.qos.logback.core.Context;

public class FileNamePattern extends ch.qos.logback.core.rolling.helper.FileNamePattern {

  public FileNamePattern(String patternArg, Context contextArg) {
    super(patternArg, contextArg);
  }

  @Override
  public void setPattern(String pattern) {
    super.setPattern(escapeDirectory(pattern));
  }

  // visible for testing
  static String escapeDirectory(String pattern) {
    int index = pattern.lastIndexOf('/');
    if (index == -1) {
      return pattern;
    }
    String directory = pattern.substring(0, index + 1);
    String file = pattern.substring(index + 1);
    return directory.replace("%", "\\%") + file;
  }
}
