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

package com.microsoft.applicationinsights.internal.channel.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.channel.TransmissionDispatcher;
import com.microsoft.applicationinsights.internal.channel.TransmissionsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class is responsible for loading transmission files that were saved to the disk
 *
 * The class will ask for the oldest transmission file and will hand it to the dispatcher
 *
 * Created by gupele on 12/22/2014.
 */
public final class ActiveTransmissionLoader implements TransmissionsLoader {

    private static final Logger logger = LoggerFactory.getLogger(ActiveTransmissionLoader.class);

    public static final int MAX_THREADS_ALLOWED = 10;

    private static final int DEFAULT_NUMBER_OF_THREADS = 1;

    private static final long DEFAULT_SLEEP_INTERVAL_WHEN_NO_TRANSMISSIONS_FOUND_IN_MILLS = 2000;
    private static final long DEFAULT_SLEEP_INTERVAL_AFTER_DISPATCHING_IN_MILLS = 100;

    // The helper class that encapsulates the file system access
    private final TransmissionFileSystemOutput fileSystem;

    // A synchronized flag to let us know when to stop
    private final AtomicBoolean done = new AtomicBoolean(false);

    // The dispatcher is needed to process the fetched Transmissions
    private final TransmissionDispatcher dispatcher;

    private final CountDownLatch latch;

    private final TransmissionPolicyStateFetcher transmissionPolicyFetcher;

    // The threads that do the work
    private final Thread[] threads;

    private final long sleepIntervalWhenNoTransmissionsFoundInMills;

    public ActiveTransmissionLoader(TransmissionFileSystemOutput fileSystem, TransmissionPolicyStateFetcher transmissionPolicy, TransmissionDispatcher dispatcher) {
        this(fileSystem, dispatcher, transmissionPolicy, DEFAULT_NUMBER_OF_THREADS);
    }

    public ActiveTransmissionLoader(final TransmissionFileSystemOutput fileSystem,
                                    final TransmissionDispatcher dispatcher,
                                    final TransmissionPolicyStateFetcher transmissionPolicy,
                                    int numberOfThreads) {
        Preconditions.checkNotNull(fileSystem, "fileSystem must be a non-null value");
        Preconditions.checkNotNull(dispatcher, "dispatcher must be a non-null value");
        Preconditions.checkNotNull(transmissionPolicy, "transmissionPolicy must be a non-null value");
        Preconditions.checkArgument(numberOfThreads > 0, "numberOfThreads must be a positive number");
        Preconditions.checkArgument(numberOfThreads < MAX_THREADS_ALLOWED, "numberOfThreads must be smaller than %s", MAX_THREADS_ALLOWED);

        // Guy: This will probably be changed once we have configuration
        this.sleepIntervalWhenNoTransmissionsFoundInMills = DEFAULT_SLEEP_INTERVAL_WHEN_NO_TRANSMISSIONS_FOUND_IN_MILLS;

        this.transmissionPolicyFetcher = transmissionPolicy;

        this.fileSystem = fileSystem;
        this.dispatcher = dispatcher;
        threads = new Thread[numberOfThreads];
        latch = new CountDownLatch(numberOfThreads);
        final String threadNameFmt = String.format("%s-worker-%%d", ActiveTransmissionLoader.class.getSimpleName());
        for (int i = 0; i < numberOfThreads; ++i) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    latch.countDown();

                    // Avoid un-expected exit of threads
                    while (!done.get()) {
                        try {
                            TransmissionPolicy currentTransmissionState = transmissionPolicyFetcher.getCurrentState();
                            switch (currentTransmissionState) {
                                case UNBLOCKED:
                                    fetchNext(true);
                                    break;
                                case BACKOFF:
                                case BLOCKED_BUT_CAN_BE_PERSISTED:
                                    Thread.sleep(DEFAULT_SLEEP_INTERVAL_AFTER_DISPATCHING_IN_MILLS);
                                    break;

                                case BLOCKED_AND_CANNOT_BE_PERSISTED:
                                    // We fetch but don't do anything with the Transmission
                                    // which means that we are cleaning the disk as needed by that policy
                                    fetchNext(false);
                                    break;

                                default:
                                    logger.error("Could not find transmission policy '{}'", currentTransmissionState);
                                    Thread.sleep(DEFAULT_SLEEP_INTERVAL_AFTER_DISPATCHING_IN_MILLS);
                                    break;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (ThreadDeath td) {
                            throw td;
                        } catch (Throwable t) {
                            // chomp
                        }
                    }
                }
            }, String.format(threadNameFmt, i));
            threads[i].setDaemon(true);
        }}

    @Override
    public synchronized boolean load(boolean waitForThreadsToStart) {
        for (Thread thread : threads) {
            thread.start();
        }

        if (!waitForThreadsToStart) {
            return true;
        }

        try {
            latch.await();
            return true;
        } catch (InterruptedException e) {
            logger.error("Interrupted waiting for threads to start: {}", e.toString());
            Thread.currentThread().interrupt();
        }

        return false;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        done.set(true);
        interruptAllThreads();
        joinAllThreads();
    }

    private void joinAllThreads() {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error("Interrupted during join of active transmission loader, exception: {}", e.toString());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void interruptAllThreads() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    private void fetchNext(boolean shouldDispatch) throws InterruptedException {
        Transmission transmission = fileSystem.fetchOldestFile();
        if (transmission == null) {
            Thread.sleep(sleepIntervalWhenNoTransmissionsFoundInMills);
        } else {
            if (shouldDispatch) {
                dispatcher.dispatch(transmission);
            }

            Thread.sleep(DEFAULT_SLEEP_INTERVAL_AFTER_DISPATCHING_IN_MILLS);
        }
    }
}
