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

package com.microsoft.applicationinsights.agent.internal.common;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemInformation {

  private static final Logger logger = LoggerFactory.getLogger(SystemInformation.class);

  private static final String DEFAULT_PROCESS_NAME = "Java_Process";

  private static final boolean WINDOWS;
  private static final boolean LINUX;

  static {
    String osName = System.getProperty("os.name");
    String osNameLower = osName == null ? null : osName.toLowerCase(Locale.ENGLISH);
    WINDOWS = osNameLower != null && osNameLower.startsWith("windows");
    LINUX = osNameLower != null && osNameLower.startsWith("linux");
  }

  private static final String processId = initializeProcessId();

  public static String getProcessId() {
    return processId;
  }

  public static boolean isWindows() {
    return WINDOWS;
  }

  public static boolean isLinux() {
    return LINUX;
  }

  /**
   * JVMs are not required to publish this value/bean and some processes may not have permission to
   * access it.
   */
  private static String initializeProcessId() {
    String rawName = ManagementFactory.getRuntimeMXBean().getName();
    if (!Strings.isNullOrEmpty(rawName)) {
      int i = rawName.indexOf("@");
      if (i != -1) {
        String processIdAsString = rawName.substring(0, i);
        try {
          Integer.parseInt(processIdAsString);
          return processIdAsString;
        } catch (RuntimeException e) {
          logger.error("Failed to fetch process id: '{}'", e.toString());
          logger.error("Failed to parse PID as number: '{}'", e.toString());
          logger.debug(e.getMessage(), e);
        }
      }
    }
    logger.error("Could not extract PID from runtime name: '" + rawName + "'");
    // Default
    return DEFAULT_PROCESS_NAME;
  }

  private SystemInformation() {}
}
