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

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

public class InstrumentationKeyFinder implements DiagnosticsValueFinder {

  private static final String PREFIX = "InstrumentationKey=";

  @Override
  public String getName() {
    return "ikey";
  }

  @Override
  public String getValue() {
    String connStr = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");
    if (connStr != null && !connStr.isEmpty()) {
      String[] parts = connStr.split(";");
      String instrumentationKey = null;
      for (String part : parts) {
        String trimmed = part.trim();
        if (trimmed.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
          instrumentationKey = trimmed.substring(PREFIX.length());
        }
      }
      // return the last instrumentation key to match ConnectionString::parseInto
      return instrumentationKey;
    }
    return System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY");
  }
}
