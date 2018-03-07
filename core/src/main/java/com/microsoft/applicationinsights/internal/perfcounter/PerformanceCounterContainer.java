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

package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.extensibility.PerformanceCountersCollectionPlugin;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The class serves as the container of all {@link com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter}
 *
 * If there is a need for a performance counter, the user of this class should create an implementation of that interface
 * and then register it in this container.
 *
 * Note that the container will only start working after the first registration of a Performance Counter.
 * That means that setting the timeouts is only relevant if done before the first registration of a Performance Counter.
 *
 * The container will go through all the registered Performance Counters and will trigger their 'report' method.
 * By default the container will start reporting after 5 minutes and will continue doing so every 1 minute.
 *
 * The user of this class can add (register), remove (unregister) a performance counter while the container is working.
 *
 * The container will be stopped automatically when the application exists.
 *
 * Created by gupele on 3/3/2015.
 */
public enum PerformanceCounterContainer implements Stoppable {
    INSTANCE;

    // By default the container will wait 2 minutes before the collection of performance data.
    private final static long START_COLLECTING_DELAY_IN_MILLIS = 60000;
    private final static long START_DEFAULT_MIN_DELAY_IN_MILLIS = 20000;

    // By default the container will collect performance data every 1 minute.
    private final static long DEFAULT_COLLECTION_FREQUENCY_IN_SEC = 60;
    private final static long MIN_COLLECTION_FREQUENCY_IN_SEC = 1;

    private final ConcurrentMap<String, PerformanceCounter> performanceCounters = new ConcurrentHashMap<String, PerformanceCounter>();

    private volatile boolean initialized = false;

    private PerformanceCountersCollectionPlugin plugin;

    private long startCollectingDelayInMillis = START_COLLECTING_DELAY_IN_MILLIS;
    private long collectionFrequencyInMS = DEFAULT_COLLECTION_FREQUENCY_IN_SEC * 1000;

    private TelemetryClient telemetryClient;

    private ScheduledThreadPoolExecutor threads;

    /**
     /**
     * Registers a {@link com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter} that can collect data.
     * @param performanceCounter The Performance Counter.
     * @return True on success.
     */
    public boolean register(PerformanceCounter performanceCounter) {
        Preconditions.checkNotNull(performanceCounter, "performanceCounter should be non null, non empty value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(performanceCounter.getId()), "performanceCounter's id should be non null, non empty value");

        initialize();

        InternalLogger.INSTANCE.trace("Registering PC '%s'", performanceCounter.getId());
        PerformanceCounter prev = performanceCounters.putIfAbsent(performanceCounter.getId(), performanceCounter);
        if (prev != null) {
            InternalLogger.INSTANCE.trace("Failed to store performance counter '%s', since there is already one", performanceCounter.getId());
            return false;
        }

        return true;
    }

    /**
     * Un-registers a performance counter.
     * @param performanceCounter The Performance Counter.
     */
    public void unregister(PerformanceCounter performanceCounter) {
        unregister(performanceCounter.getId());
    }

    /**
     * Un-registers a performance counter by its id.
     * @param id The Performance Counter's id.
     */
    public void unregister(String id) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id should be non null, non empty value");

        InternalLogger.INSTANCE.trace("Un-registering PC '%s'", id);
        performanceCounters.remove(id);
    }

    /**
     * Gets the timeout in milliseconds that the container will wait before the first collection of Performance Counters.
     * @return The first timeout in milliseconds.
     */
    public long getStartCollectingDelayInMillis() {
        return startCollectingDelayInMillis;
    }

    /**
     * Gets the timeout in milliseconds that the container will wait between collections of Performance Counters.
     * @return The timeout between collections.
     */
    public long getCollectionFrequencyInSec() {
        return collectionFrequencyInMS / 1000;
    }

    /**
     * Stopping the collection of performance data.
     * @param timeout The timeout to wait for the stop to happen.
     * @param timeUnit The time unit to use when waiting for the stop to happen.
     */
    public synchronized void stop(long timeout, TimeUnit timeUnit) {
        if (!initialized) {
            return;
        }

        ThreadPoolUtils.stop(threads, timeout, timeUnit);
        initialized = false;
    }

    /**
     * Sets the timeout to wait between collection of Performance Counters.
     *
     * The number must be a positive number
     *
     * Note that the method will be effective if called before the first call to the 'register' method.
     * @param collectionFrequencyInSec The timeout to wait between collection of Performance Counters.
     */
    public void setCollectionFrequencyInSec(long collectionFrequencyInSec) {
        if (collectionFrequencyInSec <= MIN_COLLECTION_FREQUENCY_IN_SEC) {
            String errorMessage = String.format("Collecting Interval: illegal value '%d'. The minimum value, '%d', is used instead.", collectionFrequencyInSec, MIN_COLLECTION_FREQUENCY_IN_SEC);
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, errorMessage);

            collectionFrequencyInSec = MIN_COLLECTION_FREQUENCY_IN_SEC;
        }

        this.collectionFrequencyInMS = collectionFrequencyInSec * 1000;
    }

    /**
     * Sets the timeout to wait before the first reporting.
     *
     * The number must be a positive number
     *
     * Note that the method will be effective if called before the first call to the 'register' method.
     * @param startCollectingDelayInMillis Timeout to wait before the first collection of performance counters in milliseconds.
     */
    void setStartCollectingDelayInMillis(long startCollectingDelayInMillis) {
        if (startCollectingDelayInMillis < START_DEFAULT_MIN_DELAY_IN_MILLIS) {
            InternalLogger.INSTANCE.error("Start Collecting Delay: illegal value '%d'. The minimum value, '%'d, is used instead.", startCollectingDelayInMillis, START_DEFAULT_MIN_DELAY_IN_MILLIS);

            startCollectingDelayInMillis = START_DEFAULT_MIN_DELAY_IN_MILLIS;
        }

        this.startCollectingDelayInMillis = startCollectingDelayInMillis;
    }

    void clear() {
        performanceCounters.clear();
    }

    /**
     * A private method that is called only when the container needs to start
     * collecting performance counters data. The method will schedule a callback
     * to be called, it will initialize a {@link com.microsoft.applicationinsights.TelemetryClient} that the Performance Counters
     * will use to report their data, and it will also register itself a the {@link com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity}
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
                        if (telemetryClient == null) {
                            telemetryClient = new TelemetryClient();
                        }

                        if (plugin != null) {
                            try {
                                plugin.preCollection();
                            } catch (Throwable t) {
                                InternalLogger.INSTANCE.error("Error in thread scheduled for PerformanceCounterContainer");
                                InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
                            }
                        }

                        for (PerformanceCounter performanceCounter : performanceCounters.values()) {
                            try {
                                performanceCounter.report(telemetryClient);
                            } catch (Throwable e) {
                                InternalLogger.INSTANCE.error("Exception while reporting performance counter '%s': '%s'", performanceCounter.getId(), e.toString());
                                InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
                            }
                        }

                        if (plugin != null) {
                            try {
                                plugin.postCollection();
                            } catch (Throwable t) {
                                InternalLogger.INSTANCE.error("Error while executing post collection");
                                InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
                            }
                        }
                    }
                },
                startCollectingDelayInMillis,
                collectionFrequencyInMS,
                TimeUnit.MILLISECONDS);

        // Register the instance so the container is stopped when the application exits.
        SDKShutdownActivity.INSTANCE.register(INSTANCE);
    }

    private void createThreadToCollect() {
        threads = new ScheduledThreadPoolExecutor(1);
        threads.setThreadFactory(ThreadPoolUtils.createDaemonThreadFactory(PerformanceCounterContainer.class));
    }

    public void setPlugin(PerformanceCountersCollectionPlugin plugin) {
        this.plugin = plugin;
    }
}
