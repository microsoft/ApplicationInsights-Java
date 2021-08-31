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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/** This class manages loading a list of {@link ByteBuffer} from the disk. */
public class LocalFileLoader {

  private static final String TEMPORARY_FILE_EXTENSION = ".tmp";

  private final LocalFileCache localFileCache;
  private final File telemetryFolder;

  private static final OperationLogger operationLogger =
      new OperationLogger(LocalFileLoader.class, "Loading telemetry from disk");

  public LocalFileLoader(LocalFileCache localFileCache, File telemetryFolder) {
    this.localFileCache = localFileCache;
    this.telemetryFolder = telemetryFolder;
  }

  // Load ByteBuffer from persisted files on disk in FIFO order.
  @Nullable
  ByteBuffer loadTelemetriesFromDisk() {
    // TODO (heya) how does this load files from disk at startup that were left over from prior
    //  process?
    String filenameToBeLoaded = localFileCache.poll();
    if (filenameToBeLoaded == null) {
      return null;
    }

    File tempFile;
    try {
      File sourceFile = new File(telemetryFolder, filenameToBeLoaded);
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

    try {
      // TODO (heya) backoff and retry delete when it fails.
      Files.delete(tempFile.toPath());
    } catch (IOException ex) {
      // TODO (heya) track deserialization failure via Statsbeat
      operationLogger.recordFailure("Fail to read telemetry from " + tempFile.getName(), ex);
      return null;
    } catch (SecurityException ex) {
      operationLogger.recordFailure(
          "Unable to delete " + tempFile.getName() + ". Access is denied.", ex);
      return null;
    }

    operationLogger.recordSuccess();
    return ByteBuffer.wrap(result);
  }
}
