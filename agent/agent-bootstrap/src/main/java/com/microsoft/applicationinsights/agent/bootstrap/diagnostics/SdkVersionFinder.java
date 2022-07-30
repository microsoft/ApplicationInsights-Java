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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SdkVersionFinder extends CachedDiagnosticsValueFinder {

  private static volatile String value;

  @Override
  public String getName() {
    return "sdkVersion";
  }

  @Override
  protected String populateValue() {
    return value;
  }

  public static String getTheValue() {
    return value;
  }

  public static String initVersion(Path agentPath) {
    value = readVersion(agentPath);
    return value;
  }

  // this is called early during startup before logging has been initialized
  @SuppressWarnings({"CatchAndPrintStackTrace", "SystemOut"})
  private static String readVersion(Path agentPath) {
    try {
      // reading from file instead of from classpath, in order to avoid triggering jar file
      // signature verification
      try (JarFile jarFile = new JarFile(agentPath.toFile(), false)) {
        JarEntry entry = jarFile.getJarEntry("ai.sdk-version.properties");
        try (InputStream in = jarFile.getInputStream(entry)) {
          Properties props = new Properties();
          props.load(in);
          return props.getProperty("version");
        }
      }
    } catch (IOException e) {
      // this is called early during startup before logging has been initialized
      e.printStackTrace();
    }
    return "unknown";
  }
}
