// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class serves as the container of all {@link PerformanceCounter}
 *
 * <p>If there is a need for a performance counter, the user of this class should create an
 * implementation of that interface and then register it in this container.
 *
 * <p>Note that the container will only start working after the first registration of a Performance
 * Counter. That means that setting the timeouts is only relevant if done before the first
 * registration of a Performance Counter.
 *
 * <p>The container will go through all the registered Performance Counters and will trigger their
 * 'report' method. By default the container will start reporting after 5 minutes and will continue
 * doing so every 1 minute.
 *
 * <p>The user of this class can add (register), remove (unregister) a performance counter while the
 * container is working.
 *
 * <p>The container will be stopped automatically when the application exists.
 */
@SuppressWarnings("ImmutableEnumChecker")
public enum PerformanceCounterContainer {
  INSTANCE;

  private static final Logger logger = LoggerFactory.getLogger(PerformanceCounterContainer.class);

  // By default the container will collect performance data every 1 minute.
  public static final long DEFAULT_COLLECTION_FREQUENCY_IN_SEC = 60;
  private static final long MIN_COLLECTION_FREQUENCY_IN_SEC = 1;

  private final List<PerformanceCounter> performanceCounters = new CopyOnWriteArrayList<>();

  @Nullable private volatile AvailableJmxMetricLogger availableJmxMetricLogger;

  private volatile boolean initialized = false;

  private long collectionFrequencyInMillis = DEFAULT_COLLECTION_FREQUENCY_IN_SEC * 1000;

  private ScheduledThreadPoolExecutor threads;

  /**
   * Adds a {@link PerformanceCounter} that can collect data.
   *
   * @param performanceCounter The Performance Counter.
   */
  public void register(PerformanceCounter performanceCounter) {
    initialize();
    performanceCounters.add(performanceCounter);
  }

  /**
   * Sets the timeout to wait between collection of Performance Counters.
   *
   * <p>The number must be a positive number
   *
   * <p>Note that the method will be effective if called before the first call to the 'register'
   * method.
   *
   * @param collectionFrequencyInSec The timeout to wait between collection of Performance Counters.
   */
  public void setCollectionFrequencyInSec(long collectionFrequencyInSec) {
    if (collectionFrequencyInSec < MIN_COLLECTION_FREQUENCY_IN_SEC) {
      String errorMessage =
          String.format(
              "Collecting Interval: illegal value '%d'. The minimum value, '%d', "
                  + "is used instead.",
              collectionFrequencyInSec, MIN_COLLECTION_FREQUENCY_IN_SEC);
      logger.error(errorMessage);

      collectionFrequencyInSec = MIN_COLLECTION_FREQUENCY_IN_SEC;
    }

    this.collectionFrequencyInMillis = collectionFrequencyInSec * 1000;
  }

  public void setLogAvailableJmxMetrics() {
    availableJmxMetricLogger = new AvailableJmxMetricLogger();
  }

  /**
   * A private method that is called only when the container needs to start collecting performance
   * counters data. The method will schedule a callback to be called, it will initialize a {@link
   * TelemetryClient} that the Performance Counters will use to report their data
   */
  @SuppressWarnings("AlreadyChecked")
  private void initialize() {
    if (!initialized) {
      synchronized (INSTANCE) {
        if (!initialized) {
          createThreadToCollect();

          scheduleWork();

          initialized = true;
        }
      }
    }
  }

  private void scheduleWork() {
    threads.scheduleAtFixedRate(
        new Runnable() {
          @Override
          public void run() {
            if (availableJmxMetricLogger != null) {
              availableJmxMetricLogger.logAvailableJmxMetrics();
            }

            TelemetryClient telemetryClient = TelemetryClient.getActive();

            for (PerformanceCounter performanceCounter : performanceCounters) {
              try {
                performanceCounter.report(telemetryClient);
              } catch (ThreadDeath td) {
                throw td;
              } catch (Throwable t) {
                logger.error(
                    "Exception while reporting performance counter: '{}'",
                    performanceCounter.getClass().getName(),
                    t);
              }
            }
          }
        },
        collectionFrequencyInMillis,
        collectionFrequencyInMillis,
        TimeUnit.MILLISECONDS);
  }

  private void createThreadToCollect() {
    threads = new ScheduledThreadPoolExecutor(1);
    threads.setThreadFactory(
        ThreadPoolUtils.createDaemonThreadFactory(PerformanceCounterContainer.class));
  }
}
