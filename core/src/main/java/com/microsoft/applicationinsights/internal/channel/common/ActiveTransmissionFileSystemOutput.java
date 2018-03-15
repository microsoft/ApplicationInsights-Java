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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.channel.TransmissionOutput;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;

/**
 * The class is responsible for de-coupling the file persist activity.
 *
 * When this class is called it will use a thread pool's thread to do the persistence
 *
 * Created by gupele on 12/22/2014.
 */
public final class ActiveTransmissionFileSystemOutput implements TransmissionOutput {
    private static final AtomicInteger INSTANCE_ID_POOL = new AtomicInteger(1);
    private final ThreadPoolExecutor threadPool;
    private final TransmissionOutput actualOutput;
    private final TransmissionPolicyStateFetcher transmissionPolicy;
    private final int instanceId = INSTANCE_ID_POOL.getAndIncrement();

    public ActiveTransmissionFileSystemOutput(TransmissionOutput actualOutput, TransmissionPolicyStateFetcher transmissionPolicy) {
        Preconditions.checkNotNull(transmissionPolicy, "transmissionPolicy must be a non-null value");

        this.actualOutput = actualOutput;

        this.transmissionPolicy = transmissionPolicy;

        threadPool = ThreadPoolUtils.newLimitedThreadPool(1, 3, 20L, 1024);
        threadPool.setThreadFactory(ThreadPoolUtils.createDaemonThreadFactory(ActiveTransmissionFileSystemOutput.class, instanceId));
    }

    @Override
    public boolean send(final Transmission transmission) {
        // TODO: check the possibility of refactoring the 'send' and possible log on errors
        try {
            if (transmissionPolicy.getCurrentState() == TransmissionPolicy.BLOCKED_AND_CANNOT_BE_PERSISTED) {
                return false;
            }

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        actualOutput.send(transmission);
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable throwable) {
                        // Avoid un-expected exit of thread
                    }
                }
            });
            return true;

        } catch (RejectedExecutionException e) {
            // Note that currently if we cannot put the job to work we drop
            // the transmission, we need to add internal logging for that case
            // TODO: log
        } catch (Exception e) {
            // TODO: log
        }

        return false;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        actualOutput.stop(timeout, timeUnit);
        ThreadPoolUtils.stop(threadPool, timeout, timeUnit);
    }
}
