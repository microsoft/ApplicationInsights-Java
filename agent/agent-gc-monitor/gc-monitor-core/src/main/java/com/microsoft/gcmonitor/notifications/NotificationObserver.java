package com.microsoft.gcmonitor.notifications;

import com.microsoft.gcmonitor.collectors.JMXGarbageCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Provides an execution context for the observers to receive notifications off of the original thread
 */
public class NotificationObserver implements NotificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationObserver.class);

    private final LinkedBlockingQueue<NotificationJob> workQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService;

    public NotificationObserver(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public static class NotificationJob {
        final JMXGarbageCollector collector;
        final Notification notification;

        public NotificationJob(JMXGarbageCollector collector, Notification notification) {
            this.collector = collector;
            this.notification = notification;
        }
    }

    /**
     * Enqueue notification to be executed
     */
    @Override
    public void handleNotification(Notification notification, Object handback) {
        try {
            if (notification != null) {
                workQueue.put(new NotificationJob((JMXGarbageCollector) handback, notification));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process gc notification", e);
        }
    }

    /**
     * Start event loop that monitors for new notifications and processes them
     */
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
                        } catch (Exception e) {
                            LOGGER.error("Error while reading GC notification data", e);
                        }
                    }
                });
    }


}