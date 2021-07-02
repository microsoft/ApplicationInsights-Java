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
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PersistenceHelper {

  private static final Logger logger = LoggerFactory.getLogger(PersistenceHelper.class);

  // 50MB per folder for all apps.
  private static final long MAX_FILE_SIZE_IN_BYTES = 52428800; // 50MB
  static final String PERMANENT_FILE_EXTENSION = ".trn";
  static final String TEMPORARY_FILE_EXTENSION = ".tmp";
  static final String TELEMETRIES_FOLDER = "telemetries";
  static final String STATSBEAT_FOLDER = "statsbeat";

  /**
   * Windows: C:\Users\{USER_NAME}\AppData\Local\Temp\applicationinsights Linux:
   * /var/temp/applicationinsights We will store all persisted files in this folder for all apps.
   * TODO it is a good security practice to purge data after 24 hours in this folder.
   */
  static final File DEFAULT_FOLDER =
      new File(LocalFileSystemUtils.getTempDir(), "applicationinsights");

  static File createTempFile(boolean isStatsbeat) {
    File file = null;
    try {
      String prefix = System.currentTimeMillis() + "-";
      file = File.createTempFile(prefix, null, getDefaultFolder(isStatsbeat));
    } catch (IOException ex) {
      logger.error("Fail to create a temp file.", ex);
      // TODO (heya) track number of failures to create a temp file via Statsbeat
    }

    return file;
  }

  /** Rename the given file's file extension. */
  static File renameFileExtension(String filename, String fileExtension, boolean isStatsbeat) {
    File defaultFolder = getDefaultFolder(isStatsbeat);
    File sourceFile = new File(defaultFolder, filename);
    File tempFile = new File(defaultFolder, FilenameUtils.getBaseName(filename) + fileExtension);
    try {
      FileUtils.moveFile(sourceFile, tempFile);
    } catch (IOException ex) {
      logger.error("Fail to change {} to have {} extension.", filename, fileExtension, ex);
      // TODO (heya) track number of failures to rename a file via Statsbeat
      return null;
    }

    return tempFile;
  }

  /**
   * Before a list of {@link ByteBuffer} can be persisted to disk, need to make sure capacity has
   * not been reached yet.
   */
  static boolean maxFileSizeExceeded(boolean isStatsbeat) {
    long size = getTotalSizeOfPersistedFiles(isStatsbeat);
    if (size >= MAX_FILE_SIZE_IN_BYTES) {
      logger.warn(
          "Local persistent storage capacity has been reached. It's currently at {} KB. Telemetry will be lost.",
          (size / 1024));
      return false;
    }

    return true;
  }

  static File getDefaultFolder(boolean isStatsbeat) {
    File subdirectory;
    if (isStatsbeat) {
      subdirectory = new File(DEFAULT_FOLDER, STATSBEAT_FOLDER);
    } else {
      subdirectory = new File(DEFAULT_FOLDER, TELEMETRIES_FOLDER);
    }

    if (!subdirectory.exists()) {
      subdirectory.mkdir();
    }

    if (!subdirectory.exists() || !subdirectory.canRead() || !subdirectory.canWrite()) {
      throw new IllegalArgumentException(
          "subdirectory must exist and have read and write permissions.");
    }

    return subdirectory;
  }

  private static long getTotalSizeOfPersistedFiles(boolean isStatsbeat) {
    File defaultFolder = getDefaultFolder(isStatsbeat);
    if (!defaultFolder.exists()) {
      return 0;
    }

    long sum = 0;
    Collection<File> files =
        FileUtils.listFiles(defaultFolder, new String[] {PERMANENT_FILE_EXTENSION}, false);
    for (File file : files) {
      sum += file.length();
    }

    return sum;
  }

  private PersistenceHelper() {}
}
