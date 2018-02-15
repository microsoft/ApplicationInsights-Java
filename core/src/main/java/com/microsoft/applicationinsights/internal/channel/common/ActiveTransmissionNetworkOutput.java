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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;

/**
 * Created by gupele on 12/18/2014.
 */
public final class ActiveTransmissionNetworkOutput implements TransmissionOutput {
    private final static int DEFAULT_MAX_MESSAGES_IN_BUFFER = 128;
    private final static int DEFAULT_MIN_NUMBER_OF_THREADS = 7;
    private final static int DEFAULT_MAX_NUMBER_OF_THREADS = 7;
    private final static long DEFAULT_REMOVE_IDLE_THREAD_TIMEOUT_IN_SECONDS = 60L;

    private final int maxThreads;

    private final ThreadPoolExecutor outputThreads;

    private final TransmissionOutput actualOutput;

    private final TransmissionPolicyStateFetcher transmissionPolicy;

    public ActiveTransmissionNetworkOutput(TransmissionOutput actualOutput, TransmissionPolicyStateFetcher transmissionPolicy) {
        this(actualOutput, transmissionPolicy, DEFAULT_MAX_MESSAGES_IN_BUFFER);
    }

    public ActiveTransmissionNetworkOutput(TransmissionOutput actualOutput, TransmissionPolicyStateFetcher transmissionPolicy, int maxMessagesInBuffer) {
        Preconditions.checkNotNull(transmissionPolicy, "transmissionPolicy must be a valid non-null value");

        this.actualOutput = actualOutput;
        this.transmissionPolicy = transmissionPolicy;

        maxThreads = DEFAULT_MAX_NUMBER_OF_THREADS;
        outputThreads = ThreadPoolUtils.newLimitedThreadPool(
                DEFAULT_MIN_NUMBER_OF_THREADS,
                maxThreads,
                DEFAULT_REMOVE_IDLE_THREAD_TIMEOUT_IN_SECONDS,
                maxMessagesInBuffer);
        final String threadNameFmt = String.format("%s-job-%%d", ActiveTransmissionNetworkOutput.class.getSimpleName());
        outputThreads.setThreadFactory(new ThreadFactory() {
            private final AtomicInteger threadId = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, String.format(threadNameFmt, threadId.getAndIncrement()));
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    public boolean send(final Transmission transmission) {
        try {
            if (transmissionPolicy.getCurrentState() != TransmissionPolicy.UNBLOCKED) {
                return false;
            }

            outputThreads.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        actualOutput.send(transmission);
                    } catch (Throwable throwable) {
                        // Avoid un-expected exit of thread
                    }
                }
            });
            return true;

        } catch (RejectedExecutionException e) {
        } catch (Exception e) {
            // TODO: log
        }

        return false;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        actualOutput.stop(timeout, timeUnit);
        ThreadPoolUtils.stop(outputThreads, timeout, timeUnit);
    }

    public int getNumberOfMaxThreads() {
        return this.maxThreads;
    }
}

