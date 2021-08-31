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

package com.microsoft.applicationinsights.serviceprofilerapi.client.uploader;

import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.OsPlatforms;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OsPlatformProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OsPlatformProvider.class.getName());

  @Nullable
  public static String getOsPlatformDescription() {
    if (isWindows()) {
      return OsPlatforms.WINDOWS;
    } else if (isLinux()) {
      return OsPlatforms.LINUX;
    } else if (isMac()) {
      return OsPlatforms.OSX;
    }

    LOGGER.warn("Type of operating system could not be determined");
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
