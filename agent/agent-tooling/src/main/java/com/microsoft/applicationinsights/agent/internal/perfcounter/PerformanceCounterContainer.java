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

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
public enum PerformanceCounterContainer {
  INSTANCE;

  private static final Logger logger = LoggerFactory.getLogger(PerformanceCounterContainer.class);

  // By default the container will wait 2 minutes before the collection of performance data.
  private static final long START_COLLECTING_DELAY_IN_MILLIS = 60000;

  // By default the container will collect performance data every 1 minute.
  public static final long DEFAULT_COLLECTION_FREQUENCY_IN_SEC = 60;
  private static final long MIN_COLLECTION_FREQUENCY_IN_SEC = 1;

  private final ConcurrentMap<String, PerformanceCounter> performanceCounters =
      new ConcurrentHashMap<>();

  private volatile boolean initialized = false;

  private long collectionFrequencyInMillis = DEFAULT_COLLECTION_FREQUENCY_IN_SEC * 1000;

  private ScheduledThreadPoolExecutor threads;

  /**
   * /** Registers a {@link PerformanceCounter} that can collect data.
   *
   * @param performanceCounter The Performance Counter.
   * @return True on success.
   */
  public boolean register(PerformanceCounter performanceCounter) {
    initialize();

    logger.trace("Registering PC '{}'", performanceCounter.getId());
    PerformanceCounter prev =
        performanceCounters.putIfAbsent(performanceCounter.getId(), performanceCounter);
    if (prev != null) {
      logger.trace(
          "Failed to store performance counter '{}', since there is already one",
          performanceCounter.getId());
      return false;
    }

    return true;
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

  /**
   * A private method that is called only when the container needs to start collecting performance
   * counters data. The method will schedule a callback to be called, it will initialize a {@link
   * TelemetryClient} that the Performance Counters will use to report their data
   */
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
            TelemetryClient telemetryClient = TelemetryClient.getActive();

            for (PerformanceCounter performanceCounter : performanceCounters.values()) {
              try {
                performanceCounter.report(telemetryClient);
              } catch (ThreadDeath td) {
                throw td;
              } catch (Throwable t) {
                try {
                  logger.error(
                      "Exception while reporting performance counter '{}'",
                      performanceCounter.getId(),
                      t);
                } catch (ThreadDeath td) {
                  throw td;
                } catch (Throwable t2) {
                  // chomp
                }
              }
            }
          }
        },
        START_COLLECTING_DELAY_IN_MILLIS,
        collectionFrequencyInMillis,
        TimeUnit.MILLISECONDS);
  }

  private void createThreadToCollect() {
    threads = new ScheduledThreadPoolExecutor(1);
    threads.setThreadFactory(
        ThreadPoolUtils.createDaemonThreadFactory(PerformanceCounterContainer.class));
  }
}
