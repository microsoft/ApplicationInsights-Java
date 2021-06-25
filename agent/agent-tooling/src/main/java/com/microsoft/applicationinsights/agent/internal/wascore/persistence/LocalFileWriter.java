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

package com.microsoft.applicationinsights.agent.internal.wascore.persistence;

import com.microsoft.applicationinsights.agent.internal.wascore.util.ExceptionStats;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class manages writing a list of {@link ByteBuffer} to the file system. */
public final class LocalFileWriter {

  private static final Logger logger = LoggerFactory.getLogger(LocalFileWriter.class);

  private final LocalFileCache localFileCache;

  private static final ExceptionStats diskExceptionStats =
      new ExceptionStats(
          PersistenceHelper.class,
          "Unable to store telemetry to disk (telemetry will be discarded):");

  public LocalFileWriter(LocalFileCache localFileCache) {
    if (!PersistenceHelper.DEFAULT_FOLDER.exists()) {
      PersistenceHelper.DEFAULT_FOLDER.mkdir();
    }

    if (!PersistenceHelper.DEFAULT_FOLDER.exists()
        || !PersistenceHelper.DEFAULT_FOLDER.canRead()
        || !PersistenceHelper.DEFAULT_FOLDER.canWrite()) {
      throw new IllegalArgumentException(
          PersistenceHelper.DEFAULT_FOLDER + " must exist and have read and write permissions.");
    }

    this.localFileCache = localFileCache;
  }

  public boolean writeToDisk(List<ByteBuffer> buffers) {
    if (!PersistenceHelper.maxFileSizeExceeded()) {
      return false;
    }

    File tempFile = PersistenceHelper.createTempFile();
    if (tempFile == null) {
      return false;
    }

    if (!write(tempFile, buffers)) {
      return false;
    }

    File permanentFile =
        PersistenceHelper.renameFileExtension(
            tempFile.getName(), PersistenceHelper.PERMANENT_FILE_EXTENSION);
    if (permanentFile == null) {
      return false;
    }

    localFileCache.addPersistedFilenameToMap(permanentFile.getName());

    logger.info(
        "List<ByteBuffers> has been persisted to file and will be sent when the network becomes available.");
    // TODO (heya) track data persistence success via Statsbeat
    diskExceptionStats.recordSuccess();
    return true;
  }

  private static boolean write(File file, List<ByteBuffer> buffers) {
    try (FileChannel channel = new FileOutputStream(file).getChannel()) {
      for (ByteBuffer byteBuffer : buffers) {
        channel.write(byteBuffer);
      }
      return true;
    } catch (IOException ex) {
      // TODO (heya) track IO write failure via Statsbeat
      diskExceptionStats.recordFailure(String.format("unable to write to file: %s", ex), ex);
      return false;
    }
  }
}
