// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor.notifications;

import com.microsoft.gcmonitor.collectors.JmxGarbageCollectorStats;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.management.Notification;
import javax.management.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an execution context for the observers to receive notifications off of the original
 * thread.
 */
public class NotificationObserver implements NotificationListener {
  private static final Logger logger = LoggerFactory.getLogger(NotificationObserver.class);
  private static final int MAX_QUEUE_SIZE = 1000;
  private final LinkedBlockingQueue<NotificationJob> workQueue =
      new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
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
  public void handleNotification(@Nullable Notification notification, Object handback) {
    try {
      if (notification != null) {
        workQueue.offer(new NotificationJob((JmxGarbageCollectorStats) handback, notification));
      }
    } catch (RuntimeException e) {
      logger.error("Failed to process gc notification", e);
    }
  }

  /** Start event loop that monitors for new notifications and processes them. */
  public void watchGcNotificationEvents() {
    executorService.submit(
        () -> {
          try {
            //noinspection InfiniteLoopStatement
            while (true) {
              try {
                NotificationJob sample = workQueue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);
                if (sample != null) {
                  sample.collector.update(sample.notification);
                }
              } catch (RuntimeException e) {
                logger.error("Error while reading GC notification data", e);
              }
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
          }
        });
  }
}
