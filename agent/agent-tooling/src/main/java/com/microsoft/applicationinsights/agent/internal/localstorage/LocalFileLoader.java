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
import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/** This class manages loading a list of {@link ByteBuffer} from the disk. */
public class LocalFileLoader {

  private static final String TEMPORARY_FILE_EXTENSION = ".tmp";

  private static final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadPoolUtils.createDaemonThreadFactory(LocalFileLoader.class));

  private final LocalFileCache localFileCache;
  private final File telemetryFolder;

  private static final OperationLogger operationLogger =
      new OperationLogger(LocalFileLoader.class, "Loading telemetry from disk");

  private final Queue<File> toBeDeletedFileQueue = new ConcurrentLinkedDeque<>();

  public LocalFileLoader(LocalFileCache localFileCache, File telemetryFolder) {
    this(localFileCache, telemetryFolder, null, null);
  }

  // purgeIntervalSeconds and expiredIntervalSeconds are used by tests only
  public LocalFileLoader(
      LocalFileCache localFileCache,
      File telemetryFolder,
      @Nullable Long purgeIntervalSeconds,
      @Nullable Long expiredIntervalSeconds) {
    this.localFileCache = localFileCache;
    this.telemetryFolder = telemetryFolder;

    // run purge task daily to clean up expired files that are older than 48 hours.
    long interval =
        purgeIntervalSeconds == null ? TimeUnit.DAYS.toSeconds(1) : purgeIntervalSeconds;
    long expiredInterval =
        expiredIntervalSeconds == null ? TimeUnit.DAYS.toSeconds(2) : expiredIntervalSeconds;
    scheduledExecutor.scheduleWithFixedDelay(
        new PurgeExpiredFilesTask(expiredInterval), interval, interval, TimeUnit.SECONDS);
  }

  // Load ByteBuffer from persisted files on disk in FIFO order.
  ByteBuffer loadTelemetriesFromDisk() {
    String filenameToBeLoaded = localFileCache.poll();
    if (filenameToBeLoaded == null) {
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

    toBeDeletedFileQueue.add(sourceFile);
    operationLogger.recordSuccess();
    return ByteBuffer.wrap(result);
  }

  // delete a file on the queue permanently when http response returns success.
  public void deleteFilePermanentlyOnSuccess() {
    File file = toBeDeletedFileQueue.poll();

    if (!file.exists()) {
      return;
    }

    try {
      Files.delete(file.toPath());
    } catch (IOException ex) {
      // TODO (heya) track file deletion failure via Statsbeat
      operationLogger.recordFailure("Fail to delete " + file.getName(), ex);
      retryDelete(file);
    } catch (SecurityException ex) {
      operationLogger.recordFailure(
          "Unable to delete " + file.getName() + ". Access is denied.", ex);
    }
  }

  // retry delete 3 times when it fails.
  private static void retryDelete(File file) {
    if (file.exists()) {
      for (int i = 0; i < 3; i++) {
        try {
          Thread.sleep(500);
          System.gc();
          if (file.delete()) {
            break;
          }
        } catch (InterruptedException ex) {
          operationLogger.recordFailure("Fail to perform Thread.sleep.", ex);
        }
      }
    }
  }

  private class PurgeExpiredFilesTask implements Runnable {

    private final long expiredIntervalSeconds;

    private PurgeExpiredFilesTask(long expiredIntervalSeconds) {
      this.expiredIntervalSeconds = expiredIntervalSeconds;
    }

    @Override
    public void run() {
      purgedExpiredFiles();
    }

    private void purgedExpiredFiles() {
      Collection<File> files = FileUtils.listFiles(telemetryFolder, new String[] {"trn"}, false);
      for (File file : files) {
        if (expired(file.getName())) {
          try {
            Files.delete(file.toPath());
          } catch (IOException ex) {
            operationLogger.recordFailure("Fail to delete the expired " + file.getName(), ex);
            retryDelete(file);
          }
        }
      }
    }

    // files that are older than expiredIntervalSeconds (default 48 hours) are expired and need to
    // be deleted permanently.
    private boolean expired(String fileName) {
      String time = fileName.substring(0, fileName.lastIndexOf('-'));
      long milliseconds = Long.parseLong(time);
      Date twoDaysAgo = new Date(System.currentTimeMillis() - 1000 * expiredIntervalSeconds);
      Date fileDate = new Date(milliseconds);
      return fileDate.before(twoDaysAgo);
    }
  }
}
