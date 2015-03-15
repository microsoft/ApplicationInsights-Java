/*
 * AppInsights-Java
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
import java.util.concurrent.ThreadFactory;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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
    private final static long START_COLLECTING_INTERVAL_IN_MILLIS = 30000;// 120000;

    // By default the container will collect performance data every 1 minute.
    private final static long COLLECTING_INTERVAL_IN_MILLIS = 60000;

    private final ConcurrentMap<String, PerformanceCounter> performanceCounters = new ConcurrentHashMap<String, PerformanceCounter>();

    private volatile boolean initialized = false;

    private long startCollectingIntervalInMillis = START_COLLECTING_INTERVAL_IN_MILLIS;
    private long collectingIntervalInMillis = COLLECTING_INTERVAL_IN_MILLIS;

    private TelemetryClient telemetryClient;

    private ScheduledThreadPoolExecutor threads;

    /**
     * Registers a {@link com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter} that can collect data.
     * @param performanceCounter The Performance Counter.
     */
    public void register(PerformanceCounter performanceCounter) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(performanceCounter.getId()), "performanceCounter's id should be non null, non empty value");

        initialize();

        InternalLogger.INSTANCE.trace("Registering PC '%s'", performanceCounter.getId());
        performanceCounters.put(performanceCounter.getId(), performanceCounter);
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
    public long getStartCollectingIntervalInMillis() {
        return startCollectingIntervalInMillis;
    }

    /**
     * Gets the timeout in milliseconds that the container will wait between collections of Performance Counters.
     * @return The timeout between collections.
     */
    public long getCollectingIntervalInMillis() {
        return collectingIntervalInMillis;
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
     * Sets the timeout to wait before the first reporting.
     *
     * The number must be a positive number
     *
     * Note that the method will be effective if called before the first call to the 'register' method.
     * @param startCollectingIntervalInMillis Timeout to wait before the first collection of performance counters in milliseconds.
     */
    void setStartCollectingIntervalInMillis(long startCollectingIntervalInMillis) {
        if (startCollectingIntervalInMillis <= 0) {
            InternalLogger.INSTANCE.error("'%d' is an illegal value is ignored. Must be a positive number", startCollectingIntervalInMillis);
            return;
        }

        this.startCollectingIntervalInMillis = startCollectingIntervalInMillis;
    }

    /**
     * Sets the timeout to wait between collection of Performance Counters.
     *
     * The number must be a positive number
     *
     * Note that the method will be effective if called before the first call to the 'register' method.
     * @param collectingIntervalInMillis The timeout to wait between collection of Performance Counters.
     */
    void setCollectingIntervalInMillis(long collectingIntervalInMillis) {
        if (collectingIntervalInMillis <= 0) {
            InternalLogger.INSTANCE.error("'%d' is an illegal value is ignored. Must be a positive number", collectingIntervalInMillis);
            return;
        }

        this.collectingIntervalInMillis = collectingIntervalInMillis;
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

                        for (PerformanceCounter performanceCounter : performanceCounters.values()) {
                            try {
                                performanceCounter.report(telemetryClient);
                            } catch (Throwable e) {
                                InternalLogger.INSTANCE.error("Exception while reporting performance counter '%s': '%s'", performanceCounter.getId(), e.getMessage());
                            }
                        }
                    }
                },
                startCollectingIntervalInMillis,
                collectingIntervalInMillis,
                TimeUnit.MILLISECONDS);

        // Register the instance so the container is stopped when the application exits.
        SDKShutdownActivity.INSTANCE.register(INSTANCE);
    }

    private void createThreadToCollect() {
        threads = new ScheduledThreadPoolExecutor(1);
        threads.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });
    }
}
