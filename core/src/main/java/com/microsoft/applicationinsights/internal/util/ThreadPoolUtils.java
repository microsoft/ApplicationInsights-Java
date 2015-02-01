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
                    InternalLogger.INSTANCE.trace("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
