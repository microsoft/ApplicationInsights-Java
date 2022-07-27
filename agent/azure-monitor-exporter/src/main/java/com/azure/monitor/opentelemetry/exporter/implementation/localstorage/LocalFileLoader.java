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

package com.azure.monitor.opentelemetry.exporter.implementation.localstorage;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMdc;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.MDC;

/** This class manages loading a list of {@link ByteBuffer} from the disk. */
class LocalFileLoader {

  // A regex to validate that an instrumentation key is well-formed. It's copied straight from the
  // Breeze repo.
  private static final String INSTRUMENTATION_KEY_REGEX =
      "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
  private static final String TEMPORARY_FILE_EXTENSION = ".tmp";

  private final LocalFileCache localFileCache;
  private final File telemetryFolder;
  private final LocalStorageStats stats;

  private final OperationLogger operationLogger;
  private final OperationLogger updateOperationLogger;

  LocalFileLoader(
      LocalFileCache localFileCache,
      File telemetryFolder,
      LocalStorageStats stats,
      boolean suppressWarnings) { // used to suppress warnings from statsbeat
    this.localFileCache = localFileCache;
    this.telemetryFolder = telemetryFolder;
    this.stats = stats;

    operationLogger =
        suppressWarnings
            ? OperationLogger.NOOP
            : new OperationLogger(LocalFileLoader.class, "Loading telemetry from disk");

    updateOperationLogger =
        suppressWarnings
            ? OperationLogger.NOOP
            : new OperationLogger(LocalFileLoader.class, "Updating local telemetry on disk");
  }

  // Load ByteBuffer from persisted files on disk in FIFO order.
  @Nullable
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  PersistedFile loadTelemetriesFromDisk() {
    File fileToBeLoaded = localFileCache.poll();
    if (fileToBeLoaded == null) {
      return null;
    }

    // when reading a file from the disk, loader renames the source file to "*.tmp" to prevent other
    // threads from processing the same file over and over again. this will prevent same data gets
    // sent to Application Insights more than once. after reading raw bytes from the .tmp file,
    // loader will delete the temp file when http
    // response confirms it is sent successfully; otherwise, temp file will get renamed back to the
    // source file extension.
    File tempFile;
    try {
      if (!fileToBeLoaded.exists()) {
        return null;
      }

      tempFile =
          new File(
              telemetryFolder, FileUtil.getBaseName(fileToBeLoaded) + TEMPORARY_FILE_EXTENSION);
      FileUtil.moveFile(fileToBeLoaded, tempFile);
    } catch (IOException e) {
      try (MDC.MDCCloseable ignored = AzureMonitorMdc.DISK_PERSISTENCE_READ_ERROR.makeActive()) {
        operationLogger.recordFailure(
            "Error renaming file: " + fileToBeLoaded.getAbsolutePath(), e);
      }
      stats.incrementReadFailureCount();
      return null;
    }

    if (tempFile.length() <= 36) {
      if (!FileUtil.deleteFileWithRetries(tempFile)) {
        try (MDC.MDCCloseable ignored = AzureMonitorMdc.DISK_PERSISTENCE_READ_ERROR.makeActive()) {
          operationLogger.recordFailure("Unable to delete file: " + tempFile.getAbsolutePath());
        }
      }
      return null;
    }

    byte[] ikeyBytes = new byte[36];
    int rawByteLength = (int) tempFile.length() - 36;
    byte[] telemetryBytes = new byte[rawByteLength];
    String instrumentationKey;
    try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
      readFully(fileInputStream, ikeyBytes, 36);
      instrumentationKey = new String(ikeyBytes, UTF_8);
      if (!isInstrumentationKeyValid(instrumentationKey)) {
        fileInputStream.close(); // need to close FileInputStream before delete
        if (!FileUtil.deleteFileWithRetries(tempFile)) {
          try (MDC.MDCCloseable ignored =
              AzureMonitorMdc.DISK_PERSISTENCE_READ_ERROR.makeActive()) {
            operationLogger.recordFailure("Unable to delete file: " + tempFile.getAbsolutePath());
          }
        }
        return null;
      }

      readFully(fileInputStream, telemetryBytes, rawByteLength);
    } catch (IOException e) {
      try (MDC.MDCCloseable ignored = AzureMonitorMdc.DISK_PERSISTENCE_READ_ERROR.makeActive()) {
        operationLogger.recordFailure("Error reading file: " + tempFile.getAbsolutePath(), e);
      }
      stats.incrementReadFailureCount();
      return null;
    }

    operationLogger.recordSuccess();
    return new PersistedFile(tempFile, instrumentationKey, ByteBuffer.wrap(telemetryBytes));
  }

  static boolean isInstrumentationKeyValid(String instrumentationKey) {
    return Pattern.matches(INSTRUMENTATION_KEY_REGEX, instrumentationKey.toLowerCase());
  }

  // reads bytes from a FileInputStream and allocates those into the buffer array byteArray.
  private static void readFully(FileInputStream fileInputStream, byte[] byteArray, int length)
      throws IOException {
    if (length < 0) {
      throw new IndexOutOfBoundsException();
    }

    int totalRead = 0;
    while (totalRead < length) {
      int numRead = fileInputStream.read(byteArray, totalRead, length - totalRead);
      if (numRead < 0) {
        throw new EOFException();
      }

      totalRead += numRead;
    }
  }

  // either delete it permanently on success or add it back to cache to be processed again later on
  // failure
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  void updateProcessedFileStatus(boolean successOrNonRetryableError, File file) {
    if (!file.exists()) {
      // not sure why this would happen
      try (MDC.MDCCloseable ignored = AzureMonitorMdc.DISK_PERSISTENCE_READ_ERROR.makeActive()) {
        updateOperationLogger.recordFailure("File no longer exists: " + file.getAbsolutePath());
      }
      return;
    }
    if (successOrNonRetryableError) {
      // delete a file on the queue permanently when http response returns success.
      if (!FileUtil.deleteFileWithRetries(file)) {
        try (MDC.MDCCloseable ignored = AzureMonitorMdc.DISK_PERSISTENCE_READ_ERROR.makeActive()) {
          updateOperationLogger.recordFailure("Unable to delete file: " + file.getAbsolutePath());
        }
      } else {
        updateOperationLogger.recordSuccess();
      }
    } else {
      // rename the temp file back to .trn source file extension
      File sourceFile = new File(telemetryFolder, FileUtil.getBaseName(file) + ".trn");
      try {
        FileUtil.moveFile(file, sourceFile);
      } catch (IOException e) {
        try (MDC.MDCCloseable ignored = AzureMonitorMdc.DISK_PERSISTENCE_READ_ERROR.makeActive()) {
          updateOperationLogger.recordFailure("Error renaming file: " + file.getAbsolutePath(), e);
        }
        return;
      }
      updateOperationLogger.recordSuccess();

      // add the source filename back to local file cache to be processed later.
      localFileCache.addPersistedFile(sourceFile);
    }
  }

  static class PersistedFile {
    final File file;
    final String instrumentationKey;
    final ByteBuffer rawBytes;

    PersistedFile(File file, String instrumentationKey, ByteBuffer byteBuffer) {
      if (instrumentationKey == null) {
        throw new IllegalArgumentException("instrumentation key can not be null.");
      }

      this.file = file;
      this.instrumentationKey = instrumentationKey;
      this.rawBytes = byteBuffer;
    }
  }
}
