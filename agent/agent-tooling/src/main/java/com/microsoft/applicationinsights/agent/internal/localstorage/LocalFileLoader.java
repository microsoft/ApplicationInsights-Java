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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.statsbeat.NonessentialStatsbeat;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/** This class manages loading a list of {@link ByteBuffer} from the disk. */
public class LocalFileLoader {

  // A regex to validate that an instrumentation key is well-formed. It's copied straight from the
  // Breeze repo.
  private static final String INSTRUMENTATION_KEY_REGEX =
      "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
  private static final String TEMPORARY_FILE_EXTENSION = ".tmp";

  private final LocalFileCache localFileCache;
  private final File telemetryFolder;
  // this is null for Statsbeat telemetry
  @Nullable private final NonessentialStatsbeat nonessentialStatsbeat;

  private static final OperationLogger operationLogger =
      new OperationLogger(LocalFileLoader.class, "Loading telemetry from disk");

  private static final OperationLogger updateOperationLogger =
      new OperationLogger(LocalFileLoader.class, "Updating local telemetry on disk");

  public LocalFileLoader(
      LocalFileCache localFileCache,
      File telemetryFolder,
      @Nullable NonessentialStatsbeat nonessentialStatsbeat) {
    this.localFileCache = localFileCache;
    this.telemetryFolder = telemetryFolder;
    this.nonessentialStatsbeat = nonessentialStatsbeat;
  }

  // Load ByteBuffer from persisted files on disk in FIFO order.
  @Nullable
  PersistedFile loadTelemetriesFromDisk() {
    String filenameToBeLoaded = localFileCache.poll();
    if (filenameToBeLoaded == null) {
      return null;
    }

    // when reading a file from the disk, loader renames the source file to "*.tmp" to prevent other
    // threads from processing the same file over and over again. this will prevent same data gets
    // sent to Application Insights more than once. after reading raw bytes from the .tmp file,
    // loader will delete the temp file when http
    // response confirms it is sent successfully; otherwise, temp file will get renamed back to the
    // source file extension.
    File tempFile;
    File sourceFile;
    try {
      sourceFile = new File(telemetryFolder, filenameToBeLoaded);
      if (!sourceFile.exists()) {
        return null;
      }

      tempFile =
          new File(
              telemetryFolder,
              FilenameUtils.getBaseName(filenameToBeLoaded) + TEMPORARY_FILE_EXTENSION);
      FileUtils.moveFile(sourceFile, tempFile);
    } catch (IOException e) {
      operationLogger.recordFailure(
          "Failed to change "
              + filenameToBeLoaded
              + " to have "
              + TEMPORARY_FILE_EXTENSION
              + " extension: ",
          e);
      incrementReadFailureCount();
      return null;
    }

    if (tempFile.length() <= 36) {
      if (LocalStorageUtils.deleteFileWithRetries(tempFile)) {
        operationLogger.recordFailure(
            "Fail to delete a corrupted persisted file: length is  " + tempFile.length());
      }
      return null;
    }

    byte[] ikeyBytes = new byte[36];
    int rawByteLength = (int) tempFile.length() - 36;
    byte[] telemetryBytes = new byte[rawByteLength];
    String instrumentationKey = null;
    try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
      readFully(fileInputStream, ikeyBytes, 36);
      instrumentationKey = new String(ikeyBytes, UTF_8);
      if (!isInstrumentationKeyValid(instrumentationKey)) {
        fileInputStream.close(); // need to close FileInputStream before delete
        if (!LocalStorageUtils.deleteFileWithRetries(tempFile)) {
          operationLogger.recordFailure(
              "Fail to delete the old persisted file with an invalid instrumentation key "
                  + tempFile.getName());
        }
        return null;
      }

      readFully(fileInputStream, telemetryBytes, rawByteLength);
    } catch (IOException ex) {
      operationLogger.recordFailure("Fail to read telemetry from " + tempFile.getName(), ex);
      incrementReadFailureCount();
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
  public void updateProcessedFileStatus(boolean success, File file) {
    if (!file.exists()) {
      // not sure why this would happen
      updateOperationLogger.recordFailure("File no longer exists: " + file.getName());
      return;
    }
    if (success) {
      // delete a file on the queue permanently when http response returns success.
      if (!LocalStorageUtils.deleteFileWithRetries(file)) {
        // TODO (heya) track file deletion failure via Statsbeat
        updateOperationLogger.recordFailure("Fail to delete " + file.getName());
      } else {
        updateOperationLogger.recordSuccess();
      }
    } else {
      // rename the temp file back to .trn source file extension
      File sourceFile =
          new File(telemetryFolder, FilenameUtils.getBaseName(file.getName()) + ".trn");
      try {
        FileUtils.moveFile(file, sourceFile);
      } catch (IOException ex) {
        updateOperationLogger.recordFailure(
            "Fail to rename " + file.getName() + " to have a .trn extension.", ex);
        return;
      }
      updateOperationLogger.recordSuccess();

      // add the source filename back to local file cache to be processed later.
      localFileCache.addPersistedFilenameToMap(sourceFile.getName());
    }
  }

  private void incrementReadFailureCount() {
    if (nonessentialStatsbeat != null) {
      nonessentialStatsbeat.incrementReadFailureCount();
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
