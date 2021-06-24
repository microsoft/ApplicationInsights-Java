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

package com.microsoft.gcmonitor.notifications;

import com.microsoft.gcmonitor.collectors.JmxGarbageCollectorStats;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.management.Notification;
import javax.management.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an execution context for the observers to receive notifications off of the original
 * thread.
 */
public class NotificationObserver implements NotificationListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationObserver.class);

  private final LinkedBlockingQueue<NotificationJob> workQueue = new LinkedBlockingQueue<>();
  private final ExecutorService executorService;

  public NotificationObserver(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public static class NotificationJob {
    final JmxGarbageCollectorStats collector;
    final Notification notification;

    public NotificationJob(JmxGarbageCollectorStats collector, Notification notification) {
      this.collector = collector;
      this.notification = notification;
    }
  }

  /** Enqueue notification to be executed. */
  @Override
  public void handleNotification(Notification notification, Object handback) {
    try {
      if (notification != null) {
        workQueue.put(new NotificationJob((JmxGarbageCollectorStats) handback, notification));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      LOGGER.error("Failed to process gc notification", e);
    }
  }

  /** Start event loop that monitors for new notifications and processes them. */
  public void watchGcNotificationEvents() {
    executorService.submit(
        () -> {
          //noinspection InfiniteLoopStatement
          while (true) {
            try {
              NotificationJob sample = workQueue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
              if (sample != null) {
                sample.collector.update(sample.notification);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw e;
            } catch (RuntimeException e) {
              LOGGER.error("Error while reading GC notification data", e);
            }
          }
        });
  }
}
