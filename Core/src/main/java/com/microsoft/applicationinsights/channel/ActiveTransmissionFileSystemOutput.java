package com.microsoft.applicationinsights.channel;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.util.ThreadPoolUtils;

/**
 * The class is responsible for de-coupling the file persist activity.
 *
 * When this class is called it will use a thread pool's thread to do the persistence
 *
 * Created by gupele on 12/22/2014.
 */
public class ActiveTransmissionFileSystemOutput implements TransmissionOutput {
    private final ThreadPoolExecutor threadPool;

    private final TransmissionOutput actualOutput;

    public ActiveTransmissionFileSystemOutput(TransmissionOutput actualOutput) {
        this.actualOutput = actualOutput;
        threadPool = ThreadPoolUtils.newLimitedThreadPool(1, 3, 20L, 1024);
        threadPool.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    public boolean send(final Transmission transmission) {
        // TODO: check the possibility of refactoring the 'send' and possible log on errors
        try {

            threadPool.execute(new Runnable() {
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
