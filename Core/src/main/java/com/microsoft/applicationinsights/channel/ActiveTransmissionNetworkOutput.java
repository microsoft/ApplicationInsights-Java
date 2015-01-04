package com.microsoft.applicationinsights.channel;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.util.ThreadPoolUtils;

/**
 * Created by gupele on 12/18/2014.
 */
public final class ActiveTransmissionNetworkOutput implements TransmissionOutput {
    private final static int DEFAULT_MAX_MESSAGES_IN_BUFFER = 128;
    private final static int DEFAULT_MIN_NUMBER_OF_THREADS = 1;
    private final static int DEFAULT_MAX_NUMBER_OF_THREADS = 7;
    private final static long DEFAULT_REMOVE_IDLE_THREAD_TIMEOUT_IN_SECONDS = 60L;

    private final int maxThreads;

    private final ThreadPoolExecutor outputThreads;

    private final TransmissionOutput actualOutput;

    public ActiveTransmissionNetworkOutput(TransmissionOutput actualOutput) {
        this(actualOutput, DEFAULT_MAX_MESSAGES_IN_BUFFER);
    }

    public ActiveTransmissionNetworkOutput(TransmissionOutput actualOutput, int maxMessagesInBuffer) {
        this.actualOutput = actualOutput;

        maxThreads = DEFAULT_MAX_NUMBER_OF_THREADS;
        outputThreads = ThreadPoolUtils.newLimitedThreadPool(
                DEFAULT_MIN_NUMBER_OF_THREADS,
                maxThreads,
                DEFAULT_REMOVE_IDLE_THREAD_TIMEOUT_IN_SECONDS,
                maxMessagesInBuffer);
    }

    @Override
    public boolean send(final Transmission transmission) {
        try {
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

