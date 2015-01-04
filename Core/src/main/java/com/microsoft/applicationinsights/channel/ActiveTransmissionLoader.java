package com.microsoft.applicationinsights.channel;

import com.google.common.base.Preconditions;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The class is responsible for loading transmission files that were saved to the disk
 *
 * The class will ask for the oldest transmission file and will hand it to the dispatcher
 *
 * Created by gupele on 12/22/2014.
 */
public final class ActiveTransmissionLoader implements TransmissionsLoader {
    public final static int MAX_THREADS_ALLOWED = 10;

    private final static int DEFAULT_NUMBER_OF_THREADS = 1;

    private final static long DEFAULT_SLEEP_INTERVAL_WHEN_NO_TRANSMISSIONS_FOUND_IN_MILLS = 2000;
    private final static long DEFAULT_SLEEP_INTERVAL_AFTER_DISPATCHING_IN_MILLS = 100;

    // The helper class that encapsulates the file system access
    private final TransmissionFileSystemOutput fileSystem;

    // A synchronized flag to let us know when to stop
    private final AtomicBoolean done = new AtomicBoolean(false);

    // The dispatcher is needed to process the fetched Transmissions
    private final TransmissionDispatcher dispatcher;

    private CyclicBarrier barrier;

    // The threads that do the work
    private final Thread[] threads;

    private final long sleepIntervalWhenNoTransmissionsFoundInMills;

    public ActiveTransmissionLoader(TransmissionFileSystemOutput fileSystem, TransmissionDispatcher dispatcher) {
        this(fileSystem, dispatcher, DEFAULT_NUMBER_OF_THREADS);
    }

    public ActiveTransmissionLoader(final TransmissionFileSystemOutput fileSystem, final TransmissionDispatcher dispatcher, int numberOfThreads) {
        Preconditions.checkNotNull(fileSystem, "fileSystem must be a non-null value");
        Preconditions.checkNotNull(dispatcher, "dispatcher must be a non-null value");
        Preconditions.checkArgument(numberOfThreads > 0, "numberOfThreads must be a positive number");
        Preconditions.checkArgument(numberOfThreads < MAX_THREADS_ALLOWED, "numberOfThreads must be smaller than %s", MAX_THREADS_ALLOWED);

        // Guy: This will probably be changed once we have configuration
        this.sleepIntervalWhenNoTransmissionsFoundInMills = DEFAULT_SLEEP_INTERVAL_WHEN_NO_TRANSMISSIONS_FOUND_IN_MILLS;

        this.fileSystem = fileSystem;
        this.dispatcher = dispatcher;
        threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; ++i) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }

                    // Avoid un-expected exit of threads
                    while (!done.get()) {
                        try {
                            Transmission transmission = fileSystem.fetchOldestFile();
                            if (transmission == null) {
                                Thread.sleep(sleepIntervalWhenNoTransmissionsFoundInMills);
                            } else {
                                dispatcher.dispatch(transmission);

                                // TODO: check if we need this as configuration value
                                Thread.sleep(DEFAULT_SLEEP_INTERVAL_AFTER_DISPATCHING_IN_MILLS);
                            }
                        } catch (Exception e) {
                        } catch (Throwable t) {
                        }
                        // TODO: check whether we need to pause after exception
                    }
                }
            });
        }}

    @Override
    public synchronized boolean load(boolean waitForThreadsToStart) {
        if (barrier == null) {
            int numberOfThreads = threads.length;
            if (waitForThreadsToStart) {
                ++numberOfThreads;
            }
            barrier = new CyclicBarrier(numberOfThreads);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        try {
            barrier.await();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        done.set(true);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
