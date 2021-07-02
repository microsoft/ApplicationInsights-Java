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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class manages loading a list of {@link ByteBuffer} from the disk. */
public class LocalFileLoader {

  private static final Logger logger = LoggerFactory.getLogger(LocalFileLoader.class);

  private final LocalFileCache localFileCache;
  private final boolean isStatsbeat;

  public LocalFileLoader(LocalFileCache localFileCache, boolean isStatsbeat) {
    this.localFileCache = localFileCache;
    this.isStatsbeat = isStatsbeat;
  }

  // Load ByteBuffer from persisted files on disk in FIFO order.
  ByteBuffer loadTelemetriesFromDisk() {
    String filenameToBeLoaded = localFileCache.poll();
    if (filenameToBeLoaded == null) {
      return null;
    }

    File tempFile =
        PersistenceHelper.renameFileExtension(
            filenameToBeLoaded, PersistenceHelper.TEMPORARY_FILE_EXTENSION, isStatsbeat);
    if (tempFile == null) {
      return null;
    }

    return read(tempFile);
  }

  private static ByteBuffer read(File file) {
    try {
      // TODO (trask) optimization: read this directly into ByteBuffer(s)
      byte[] result = Files.readAllBytes(file.toPath());

      // TODO (heya) backoff and retry delete when it fails.
      Files.delete(file.toPath());

      return ByteBuffer.wrap(result);
    } catch (IOException ex) {
      // TODO (heya) track deserialization failure via Statsbeat
      logger.error("Fail to deserialize objects from  {}", file.getName(), ex);
      return null;
    } catch (SecurityException ex) {
      logger.error("Unable to delete {}. Access is denied.", file.getName(), ex);
      return null;
    }
  }
}
