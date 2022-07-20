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
import com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMessageIdConstants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import org.slf4j.MDC;

/** This class manages writing a list of {@link ByteBuffer} to the file system. */
final class LocalFileWriter {

  private static final String PERMANENT_FILE_EXTENSION = ".trn";

  private final long diskPersistenceMaxSizeBytes;
  private final LocalFileCache localFileCache;
  private final File telemetryFolder;
  private final LocalStorageStats stats;

  private final OperationLogger operationLogger;

  LocalFileWriter(
      int diskPersistenceMaxSizeMb,
      LocalFileCache localFileCache,
      File telemetryFolder,
      LocalStorageStats stats,
      boolean suppressWarnings) { // used to suppress warnings from statsbeat
    this.telemetryFolder = telemetryFolder;
    this.localFileCache = localFileCache;
    this.stats = stats;
    this.diskPersistenceMaxSizeBytes = diskPersistenceMaxSizeMb * 1024L * 1024L;

    operationLogger =
        suppressWarnings
            ? OperationLogger.NOOP
            : new OperationLogger(
                LocalFileWriter.class,
                "Writing telemetry to disk (telemetry is discarded on failure)");
  }

  void writeToDisk(String instrumentationKey, List<ByteBuffer> buffers) {
    long size = getTotalSizeOfPersistedFiles(telemetryFolder);
    if (size >= diskPersistenceMaxSizeBytes) {
      MDC.put(
          AzureMonitorMessageIdConstants.MDC_MESSAGE_ID,
          String.valueOf(AzureMonitorMessageIdConstants.DISK_PERSISTENCE_WRITE_ERROR));
      operationLogger.recordFailure(
          "Local persistent storage capacity has been reached. It's currently at ("
              + (size / 1024)
              + "KB). Telemetry will be lost.");
      MDC.remove(AzureMonitorMessageIdConstants.MDC_MESSAGE_ID);
      stats.incrementWriteFailureCount();
      return;
    }

    File tempFile;
    try {
      tempFile = createTempFile(telemetryFolder);
    } catch (IOException e) {
      MDC.put(
          AzureMonitorMessageIdConstants.MDC_MESSAGE_ID,
          String.valueOf(AzureMonitorMessageIdConstants.DISK_PERSISTENCE_WRITE_ERROR));
      operationLogger.recordFailure(
          "Error creating file in directory: " + telemetryFolder.getAbsolutePath(), e);
      MDC.remove(AzureMonitorMessageIdConstants.MDC_MESSAGE_ID);
      stats.incrementWriteFailureCount();
      return;
    }

    try {
      write(tempFile, buffers, instrumentationKey);
    } catch (IOException e) {
      MDC.put(
          AzureMonitorMessageIdConstants.MDC_MESSAGE_ID,
          String.valueOf(AzureMonitorMessageIdConstants.DISK_PERSISTENCE_WRITE_ERROR));
      operationLogger.recordFailure("Error writing file: " + tempFile.getAbsolutePath(), e);
      MDC.remove(AzureMonitorMessageIdConstants.MDC_MESSAGE_ID);
      stats.incrementWriteFailureCount();
      return;
    }

    File permanentFile;
    try {
      permanentFile =
          new File(telemetryFolder, FileUtil.getBaseName(tempFile) + PERMANENT_FILE_EXTENSION);
      FileUtil.moveFile(tempFile, permanentFile);
    } catch (IOException e) {
      MDC.put(
          AzureMonitorMessageIdConstants.MDC_MESSAGE_ID,
          String.valueOf(AzureMonitorMessageIdConstants.DISK_PERSISTENCE_WRITE_ERROR));
      operationLogger.recordFailure("Error renaming file: " + tempFile.getAbsolutePath(), e);
      MDC.remove(AzureMonitorMessageIdConstants.MDC_MESSAGE_ID);
      stats.incrementWriteFailureCount();
      return;
    }

    localFileCache.addPersistedFile(permanentFile);

    operationLogger.recordSuccess();
  }

  private static void write(File file, List<ByteBuffer> buffers, String instrumentationKey)
      throws IOException {
    try (FileChannel channel = new FileOutputStream(file).getChannel()) {
      channel.write(ByteBuffer.wrap(instrumentationKey.getBytes(UTF_8)));
      for (ByteBuffer byteBuffer : buffers) {
        channel.write(byteBuffer);
      }
    }
  }

  private static File createTempFile(File telemetryFolder) throws IOException {
    String prefix = System.currentTimeMillis() + "-";
    return File.createTempFile(prefix, null, telemetryFolder);
  }

  private static long getTotalSizeOfPersistedFiles(File telemetryFolder) {
    if (!telemetryFolder.exists()) {
      return 0;
    }

    long sum = 0;
    List<File> files = FileUtil.listTrnFiles(telemetryFolder);
    for (File file : files) {
      sum += file.length();
    }

    return sum;
  }
}
