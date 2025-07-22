// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystem;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Util to identify the host operating system */
public class OperatingSystemDetector {

  private static final Logger logger = LoggerFactory.getLogger(OperatingSystemDetector.class);

  private static final String OS_NAME_PROPERTY = "os.name";
  private static final String MAC = "mac";
  private static final String LINUX = "linux";
  private static final String WINDOWS = "Windows";
  private static final String SOLARIS = "sunos";

  private static final OperatingSystem operatingSystem;

  static {
    OperatingSystem detectedOs = OperatingSystem.UNKNOWN;
    try {
      String operatingSystemString = System.getProperty(OS_NAME_PROPERTY).toLowerCase(Locale.ROOT);
      logger.debug("Detected OS " + operatingSystemString);
      if (operatingSystemString.contains(MAC)) {
        detectedOs = OperatingSystem.MAC_OS;
      } else if (operatingSystemString.contains(LINUX)) {
        detectedOs = OperatingSystem.LINUX;
      } else if (operatingSystemString.contains(WINDOWS.toLowerCase(Locale.ROOT))) {
        detectedOs = OperatingSystem.WINDOWS;
      } else if (operatingSystemString.contains(SOLARIS)) {
        detectedOs = OperatingSystem.SOLARIS;
      } else {
        detectedOs = OperatingSystem.UNKNOWN;
      }
    } catch (RuntimeException e) {
      logger.warn("Failed to detect operating system", e);
    }
    operatingSystem = detectedOs;
  }

  private OperatingSystemDetector() {}

  public static OperatingSystem getOperatingSystem() {
    return operatingSystem;
  }
}
