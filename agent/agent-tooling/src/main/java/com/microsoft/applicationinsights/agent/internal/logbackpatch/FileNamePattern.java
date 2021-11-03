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
