// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.util;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OsPlatformProvider {

  private static final Logger logger = LoggerFactory.getLogger(OsPlatformProvider.class.getName());

  private static final String WINDOWS = "Windows";
  private static final String OSX = "OSX";
  private static final String LINUX = "Linux";

  @Nullable
  public static String getOsPlatformDescription() {
    if (isWindows()) {
      return WINDOWS;
    } else if (isLinux()) {
      return LINUX;
    } else if (isMac()) {
      return OSX;
    }

    logger.warn("Type of operating system could not be determined");
    return null;
  }

  private static boolean isWindows() {
    return getOsName().startsWith("Windows");
  }

  private static boolean isLinux() {
    return getOsName().startsWith("Linux") || getOsName().startsWith("LINUX");
  }

  private static boolean isMac() {
    return getOsName().startsWith("Mac");
  }

  private static String getOsName() {
    try {
      return System.getProperty("os.name");
    } catch (SecurityException ex) {
      return "";
    }
  }

  private OsPlatformProvider() {}
}
