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

import static java.util.concurrent.TimeUnit.SECONDS;

import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purge files that are older than 48 hours in both 'telemetry' and 'statsbeat' folders. Purge is
 * run every 24 hours.
 */
public class LocalFilePurger implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(LocalFilePurger.class);

  private final long expiredInterval;
  private final File[] folders;

  public static void startPurging() {
    startPurging(
        TimeUnit.DAYS.toSeconds(1),
        TimeUnit.DAYS.toSeconds(2),
        LocalStorageUtils.getOfflineTelemetryFolder(),
        LocalStorageUtils.getOfflineStatsbeatFolder());
  }

  // this is used by tests to configure purge interval, expired interval and the test folder which
  // files are to be purged.
  static void startPurging(
      long purgeIntervalSeconds, long expiredIntervalSeconds, File... folders) {
    Executors.newSingleThreadScheduledExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(LocalFilePurger.class))
        .scheduleWithFixedDelay(
            new LocalFilePurger(expiredIntervalSeconds, folders),
            purgeIntervalSeconds < 60 ? purgeIntervalSeconds : 60,
            purgeIntervalSeconds,
            SECONDS);
  }

  LocalFilePurger(long expiredInterval, File... folders) {
    this.expiredInterval = expiredInterval;
    this.folders = folders;
  }

  @Override
  public void run() {
    for (File folder : folders) {
      purgedExpiredFiles(folder);
    }
  }

  private void purgedExpiredFiles(File folder) {
    Collection<File> files = FileUtils.listFiles(folder, new String[] {"trn"}, false);
    for (File file : files) {
      if (expired(file.getName())) {
        if (!LocalStorageUtils.deleteFileWithRetries(file)) {
          logger.warn(
              "Fail to delete the expired {} from folder '{}'.", file.getName(), folder.getName());
        }
      }
    }
  }

  // files that are older than expiredIntervalSeconds (default 48 hours) are expired and need to
  // be deleted permanently.
  private boolean expired(String fileName) {
    String time = fileName.substring(0, fileName.lastIndexOf('-'));
    long milliseconds = Long.parseLong(time);
    Date twoDaysAgo = new Date(System.currentTimeMillis() - 1000 * expiredInterval);
    Date fileDate = new Date(milliseconds);
    return fileDate.before(twoDaysAgo);
  }
}
