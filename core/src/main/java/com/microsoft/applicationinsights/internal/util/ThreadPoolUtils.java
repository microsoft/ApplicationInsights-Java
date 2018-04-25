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

package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gupele on 12/22/2014.
 */
public final class ThreadPoolUtils {

    public static ThreadPoolExecutor newLimitedThreadPool(int minNumberOfThreads, int maxNumberOfThreads, long defaultRemoveIdleThread, int bufferSize) {
        return new ThreadPoolExecutor(
                minNumberOfThreads,
                maxNumberOfThreads,
                defaultRemoveIdleThread,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(bufferSize));
    }

    /**
     * This method is used to shutdown ExecutorService
     * @param executorService The instance of ExecutorService to shutdown
     * @param timeout Max time to wait
     * @param timeUnit Timeunit for timeout
     */
    public static void stop(ExecutorService executorService, long timeout, TimeUnit timeUnit) {
        if (executorService == null) {
            return;
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout, timeUnit)) {
                executorService.shutdownNow();

                if (!executorService.awaitTermination(timeout, timeUnit)) {
                    InternalLogger.INSTANCE.trace("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * {@code poolName} will be appended with a hyphen and the threadId.
     * @param clazz The class holding the thread pool
     * @param instanceId The identifier of the instance of {@code clazz}
     */
    public static ThreadFactory createDaemonThreadFactory(final Class<?> clazz, final int instanceId) {
        return createNamedDaemonThreadFactory(String.format("%s_%d", clazz.getSimpleName(), instanceId));
    }

    /**
     * {@code poolName} will be appended with a hyphen and the unique name.
     * @param clazz The class holding the thread pool
     * @param uniqueId The identifier of the instance of {@code clazz}
     * @return
     */
    public static ThreadFactory createDaemonThreadFactory(final Class<?> clazz, final String uniqueId) {
        return createNamedDaemonThreadFactory(String.format("%s_%s", clazz.getSimpleName(), uniqueId));
    }

    public static ThreadFactory createDaemonThreadFactory(final Class<?> clazz) {
        return createNamedDaemonThreadFactory(clazz.getSimpleName());
    }

    public static ThreadFactory createNamedDaemonThreadFactory(final String poolName) {
        return new ThreadFactory(){
            private AtomicInteger threadId = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(String.format("%s-%d", poolName, threadId.getAndIncrement()));
                thread.setDaemon(true);
                return thread;
            }
        };
    }
}
