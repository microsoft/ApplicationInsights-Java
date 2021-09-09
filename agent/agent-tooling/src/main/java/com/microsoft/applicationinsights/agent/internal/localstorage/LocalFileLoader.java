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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
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

  private final Queue<File> toBeDeletedFileQueue = new ConcurrentLinkedDeque<>();

  public LocalFileLoader(LocalFileCache localFileCache, File telemetryFolder) {
    this.localFileCache = localFileCache;
    this.telemetryFolder = telemetryFolder;
  }

  // Load ByteBuffer from persisted files on disk in FIFO order.
  @Nullable
  ByteBuffer loadTelemetriesFromDisk() {
    String filenameToBeLoaded = localFileCache.poll();
    if (filenameToBeLoaded == null || filenameToBeLoaded.isEmpty()) {
      return null;
    }

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
      FileUtils.copyFile(sourceFile, tempFile);
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

    toBeDeletedFileQueue.add(
        sourceFile); // mark source file to be deleted when it's sent successfully.
    deleteFile(tempFile); // delete temp file immediately
    operationLogger.recordSuccess();
    return ByteBuffer.wrap(result);
  }

  // either delete it permanently on success or add it back to cache to be processed again later on
  // failure
  public void updateProcessedFileStatus(boolean success) {
    if (success) {
      deleteFilePermanentlyOnSuccess();
    } else {
      // add the file back to local file cache to be process later.
      localFileCache.addPersistedFilenameToMap(toBeDeletedFileQueue.poll().getName());
    }
  }

  // delete a file on the queue permanently when http response returns success.
  private void deleteFilePermanentlyOnSuccess() {
    File file = toBeDeletedFileQueue.poll();

    if (!file.exists()) {
      return;
    }

    deleteFile(file);
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
