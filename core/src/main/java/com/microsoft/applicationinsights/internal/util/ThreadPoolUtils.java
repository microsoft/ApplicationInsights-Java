package com.microsoft.applicationinsights.internal.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

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

    public static void stop(ThreadPoolExecutor threadPool, long timeout, TimeUnit timeUnit) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(timeout, timeUnit)) {
                threadPool.shutdownNow();

                if (!threadPool.awaitTermination(timeout, timeUnit)) {
                    InternalLogger.INSTANCE.log("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
