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

import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/** This class manages loading a list of {@link ByteBuffer} from the disk. */
public class LocalFileLoader {

  private static final String TEMPORARY_FILE_EXTENSION = ".tmp";

  private final LocalFileCache localFileCache;
  private final File telemetryFolder;

  private static final OperationLogger operationLogger =
      new OperationLogger(LocalFileLoader.class, "Loading telemetry from disk");

  @Nullable private File toBeDeletedFile;

  public LocalFileLoader(LocalFileCache localFileCache, File telemetryFolder) {
    this.localFileCache = localFileCache;
    this.telemetryFolder = telemetryFolder;
  }

  // Load ByteBuffer from persisted files on disk in FIFO order.
  @Nullable
  ByteBuffer loadTelemetriesFromDisk() {
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
      // TODO (heya) track number of failures to create a temp file via Statsbeat
      return null;
    }

    byte[] result;
    try {
      // TODO (trask) optimization: read this directly into ByteBuffer(s)
      result = Files.readAllBytes(tempFile.toPath());
    } catch (IOException ex) {
      // TODO (heya) track deserialization failure via Statsbeat
      operationLogger.recordFailure("Fail to read telemetry from " + tempFile.getName(), ex);
      return null;
    }

    // mark source file to be deleted when it's sent successfully.
    toBeDeletedFile = tempFile;
    operationLogger.recordSuccess();
    return ByteBuffer.wrap(result);
  }

  // either delete it permanently on success or add it back to cache to be processed again later on
  // failure
  public void updateProcessedFileStatus(boolean success) {
    if (success) {
      deleteFilePermanentlyOnSuccess();
    } else {
      // rename the temp file back to .trn source file extension
      File sourceFile =
          new File(telemetryFolder, FilenameUtils.getBaseName(toBeDeletedFile.getName()) + ".trn");
      try {
        FileUtils.moveFile(toBeDeletedFile, sourceFile);
      } catch (IOException ex) {
        operationLogger.recordFailure(
            "Fail to rename " + toBeDeletedFile.getName() + " to have a .trn extension.", ex);
      }

      // add the source filename back to local file cache to be processed later.
      localFileCache.addPersistedFilenameToMap(sourceFile.getName());
    }
  }

  // delete a file on the queue permanently when http response returns success.
  private void deleteFilePermanentlyOnSuccess() {
    if (!toBeDeletedFile.exists()) {
      return;
    }

    deleteFile(toBeDeletedFile);
  }

  private static void deleteFile(File file) {
    if (!LocalStorageUtils.deleteFileWithRetries(file)) {
      // TODO (heya) track file deletion failure via Statsbeat
      operationLogger.recordFailure("Fail to delete " + file.getName());
    } else {
      operationLogger.recordSuccess();
    }
  }
}
