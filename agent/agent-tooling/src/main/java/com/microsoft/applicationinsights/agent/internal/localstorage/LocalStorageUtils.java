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

package com.microsoft.applicationinsights.agent.internal.localstorage;

import com.microsoft.applicationinsights.agent.internal.common.LocalFileSystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

public final class LocalStorageUtils {

  public static final Logger logger = LoggerFactory.getLogger(LocalStorageUtils.class);
  /**
   * Windows: C:\Users\{USER_NAME}\AppData\Local\Temp\applicationinsights Linux:
   * /var/temp/applicationinsights We will store all persisted files in this folder for all apps.
   */
  private static final File DEFAULT_FOLDER =
      new File(LocalFileSystemUtils.getTempDir(), "applicationinsights");

  private static final String TELEMETRY_FOLDER = "telemetry";
  private static final String STATSBEAT_FOLDER = "statsbeat";

  public static File getOfflineTelemetryFolder() {
    return getTelemetryFolder(TELEMETRY_FOLDER);
  }

  public static File getOfflineStatsbeatFolder() {
    return getTelemetryFolder(STATSBEAT_FOLDER);
  }

  // retry delete 3 times when it fails.
  static void retryDelete(File file) {
    if (file.exists()) {
      for (int i = 0; i < 3; i++) {
        try {
          Thread.sleep(500);
          System.gc();
          if (file.delete()) {
            break;
          }
        } catch (InterruptedException ex) {
          logger.error("Fail to perform Thread.sleep.", ex);
        }
      }
    }
  }

  // retrieve the default folder name based on telemetry type.
  // regular telemetry is written to "applicationinsights/telemetry" folder.
  // statsbeat telemetry is written to "applicationinsights/statsbeat" folder.
  private static File getTelemetryFolder(String name) {
    File subdirectory = new File(DEFAULT_FOLDER, name);

    if (!subdirectory.exists()) {
      subdirectory.mkdirs();
    }

    if (!subdirectory.exists() || !subdirectory.canRead() || !subdirectory.canWrite()) {
      throw new IllegalArgumentException(
          "subdirectory must exist and have read and write permissions.");
    }

    return subdirectory;
  }

  private LocalStorageUtils() {}
}
